# Azure AKS Deployment (Future Use)

> **Nothing in this directory has been deployed.** These files are an isolated,
> manual Azure deployment path for future use. They do not replace or modify the
> Rancher deployment.

## Architecture

Terraform provisions only Azure infrastructure:

- one resource group in `westeurope` by default;
- a virtual network and AKS subnet;
- a managed-identity AKS cluster using Microsoft Entra/Azure RBAC;
- an autoscaling two-to-four-node `Standard_D4s_v5` pool;
- a Basic Azure Container Registry with AKS pull permission; and
- a static public IP with an Azure-provided DNS name for ingress.

The existing application and monitoring Helm charts run together in the
`team-drops` namespace. PostgreSQL, MongoDB, Prometheus, Grafana, Alertmanager,
and Loki use AKS's built-in `managed-csi` Azure Disk storage class. ingress-nginx
serves the application, and cert-manager obtains its TLS certificate through an
HTTP-01 challenge.

For quota-constrained subscriptions, set the node maximum to the number the
regional vCPU quota can actually support. AKS system pools require surge
capacity during upgrades, so keep enough additional family and regional vCPU
quota for at least one temporary node before starting an upgrade.

Reference documentation:

- [Deploy AKS with Terraform](https://learn.microsoft.com/azure/aks/learn/quick-kubernetes-deploy-terraform)
- [Integrate AKS with ACR](https://learn.microsoft.com/azure/aks/cluster-container-registry-integration)
- [Azure Disk CSI storage classes](https://learn.microsoft.com/azure/aks/create-volume-azure-disk)

The in-cluster databases provide parity with the existing environment, not a
high-availability production database design. A production migration should
evaluate managed PostgreSQL and MongoDB-compatible services separately.

## Files

| Path | Purpose |
| --- | --- |
| `infra/azure/bootstrap` | Optional remote Terraform-state storage |
| `infra/azure` | VNet, AKS, ACR, public IP, DNS label, and role assignments |
| `helm/team-drops/values-azure.yaml` | Azure application overrides |
| `helm/team-drops-monitoring/values-azure.yaml` | Azure monitoring and storage overrides |
| `scripts/azure/publish-images.sh` | Guarded ACR image build and push |
| `scripts/azure/get-credentials.sh` | Retrieves AKS credentials manually |
| `scripts/azure/deploy.sh` | Guarded ingress, TLS, monitoring, and application installation |

The Azure scripts require an explicit `--confirm`. No workflow automatically
plans, applies, or deploys Azure resources.

## Prerequisites

- An Azure subscription and permission to create resource groups, AKS, ACR,
  role assignments, networking, and public IP resources.
- Azure CLI, Terraform 1.8 or newer, Helm, kubectl, Docker Buildx, and Bash.
- A Microsoft Entra user object ID for an AKS administrator. Terraform assigns
  only that identity the Azure Kubernetes Service RBAC Cluster Admin role; do
  not use a broad organization-wide group as a shortcut.
- A trusted public IPv4 CIDR for the AKS API allowlist.
- Sufficient Azure quota for two `Standard_D4s_v5` nodes and a possible scale-out
  to four nodes.

Azure services and managed disks incur charges once `terraform apply` is run.
Review current Azure pricing and subscription quota before applying.

## 1. Bootstrap remote state

This is optional but recommended before creating the platform. It creates real
Azure resources and charges may apply; do not run it merely to validate files.

```bash
cp infra/azure/bootstrap/terraform.tfvars.example \
  infra/azure/bootstrap/terraform.tfvars
# Edit the copied values first.
terraform -chdir=infra/azure/bootstrap init
terraform -chdir=infra/azure/bootstrap plan -out=bootstrap.tfplan
terraform -chdir=infra/azure/bootstrap apply bootstrap.tfplan
terraform -chdir=infra/azure/bootstrap output -raw backend_config \
  > infra/azure/backend.hcl
```

The bootstrap module grants the configured administrator user `Storage Blob
Data Contributor` on only the generated state storage account. This data-plane
role is required by the Microsoft Entra-authenticated backend. The bootstrap
state remains local and must be protected.

## 2. Review and provision Azure infrastructure

```bash
cp infra/azure/terraform.tfvars.example infra/azure/terraform.tfvars
# Replace every example identifier, suffix, and CIDR.
terraform -chdir=infra/azure init -backend-config=backend.hcl
terraform -chdir=infra/azure fmt -check
terraform -chdir=infra/azure validate
terraform -chdir=infra/azure plan -out=azure.tfplan
```

Review the saved plan carefully. Only when deployment is intentionally approved:

```bash
terraform -chdir=infra/azure apply azure.tfplan
```

The configuration deliberately does not output kubeconfig credentials or
application secrets. Useful non-secret values are available through
`terraform -chdir=infra/azure output`.

## 3. Publish images to ACR

Export the Terraform output and an immutable application tag:

```bash
export ACR_LOGIN_SERVER="$(terraform -chdir=infra/azure output -raw acr_login_server)"
export IMAGE_TAG="sha-<full-git-commit>"
scripts/azure/publish-images.sh --confirm
```

The script builds the same five linux/amd64 images as the GHCR workflow but
pushes them under `${ACR_LOGIN_SERVER}/team-drops/`. It rejects `latest`.

## 4. Connect to AKS

```bash
export AZURE_RESOURCE_GROUP="$(terraform -chdir=infra/azure output -raw resource_group_name)"
export AKS_CLUSTER_NAME="$(terraform -chdir=infra/azure output -raw aks_cluster_name)"
scripts/azure/get-credentials.sh
```

Azure RBAC still determines whether the signed-in identity may access the
cluster. Retrieving credentials does not bypass authorization.

The Terraform configuration records AKS's 10% system-pool surge setting. With
two nodes this requires one temporary node, so obtain quota for at least six
vCPUs in the selected two-vCPU VM family before a cluster or node-image upgrade.
AKS does not allow `maxUnavailable` above zero on a system pool.

## 5. Configure secrets and deploy manually

Set the non-secret infrastructure outputs:

```bash
export ACR_LOGIN_SERVER="$(terraform -chdir=infra/azure output -raw acr_login_server)"
export APP_HOSTNAME="$(terraform -chdir=infra/azure output -raw application_hostname)"
export INGRESS_PUBLIC_IP="$(terraform -chdir=infra/azure output -raw ingress_public_ip)"
export IMAGE_TAG="sha-<full-git-commit>"
export LETSENCRYPT_EMAIL="operator@example.com"
```

Supply secrets from a password manager or protected shell session. Never add
them to `.tfvars`, Azure values files, Git, or shell scripts:

```bash
export LLM_API_KEY="..."
export POSTGRES_PASSWORD="..."
export KEYCLOAK_ADMIN_PASSWORD="..."
export KEYCLOAK_CLIENT_SECRET="..."
export GRAFANA_ADMIN_PASSWORD="..."
```

Then explicitly run:

```bash
scripts/azure/deploy.sh --confirm
```

Helm stores release data and Kubernetes stores application secrets in the
cluster. Restrict namespace and secret access even though Terraform state does
not contain these values.

## Verification and operations

```bash
kubectl -n team-drops get pods,svc,ingress,pvc
kubectl -n team-drops rollout status deployment/genai-service
kubectl -n team-drops rollout status deployment/keycloak
kubectl -n team-drops rollout status statefulset/postgres
kubectl -n team-drops rollout status statefulset/mongo
curl "https://${APP_HOSTNAME}/"
```

Grafana stays private and is accessed locally:

```bash
kubectl -n team-drops port-forward svc/team-drops-monitoring-grafana 3000:80
```

Roll back application releases independently:

```bash
helm history team-drops -n team-drops
helm rollback team-drops <revision> -n team-drops
helm history team-drops-monitoring -n team-drops
helm rollback team-drops-monitoring <revision> -n team-drops
```

## Removal

Destruction is intentionally manual. Back up PostgreSQL and MongoDB first.
Deleting Helm releases or Terraform infrastructure can delete Azure Disk-backed
PVC data and cannot be assumed reversible.

```bash
helm uninstall team-drops -n team-drops
helm uninstall team-drops-monitoring -n team-drops
helm uninstall cert-manager -n cert-manager
helm uninstall ingress-nginx -n ingress-nginx
terraform -chdir=infra/azure plan -destroy -out=azure.destroy.tfplan
# Review before running: terraform -chdir=infra/azure apply azure.destroy.tfplan
```

Destroy the bootstrap state resources only after the platform state is no
longer needed and has been archived according to the project's retention policy.

## Offline validation

These commands create no Azure resources and do not contact AKS:

```bash
terraform fmt -check -recursive infra/azure
terraform -chdir=infra/azure/bootstrap init -backend=false
terraform -chdir=infra/azure/bootstrap validate
terraform -chdir=infra/azure init -backend=false
terraform -chdir=infra/azure validate

helm dependency build helm/team-drops-monitoring
helm lint helm/team-drops -f helm/team-drops/values-azure.yaml \
  --set genai.llmApiKey=dummy
helm lint helm/team-drops-monitoring -n team-drops \
  -f helm/team-drops-monitoring/values-azure.yaml
```
