#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: deploy.sh --confirm

Required environment variables:
  ACR_LOGIN_SERVER IMAGE_TAG APP_HOSTNAME AZURE_NODE_RESOURCE_GROUP
  INGRESS_PUBLIC_IP LETSENCRYPT_EMAIL LLM_API_KEY POSTGRES_PASSWORD
  KEYCLOAK_ADMIN_PASSWORD KEYCLOAK_CLIENT_SECRET GRAFANA_ADMIN_PASSWORD

The current kubectl context must be the intended AKS cluster. This script
installs ingress-nginx, cert-manager, monitoring, and the application.
EOF
}

if [[ "${1:-}" != "--confirm" ]]; then
  usage
  exit 2
fi

required_variables=(
  ACR_LOGIN_SERVER IMAGE_TAG APP_HOSTNAME AZURE_NODE_RESOURCE_GROUP
  INGRESS_PUBLIC_IP LETSENCRYPT_EMAIL LLM_API_KEY POSTGRES_PASSWORD
  KEYCLOAK_ADMIN_PASSWORD KEYCLOAK_CLIENT_SECRET GRAFANA_ADMIN_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required environment variable: ${variable_name}" >&2
    exit 2
  fi
done

if [[ "${IMAGE_TAG}" == "latest" ]]; then
  echo "IMAGE_TAG must be immutable; latest is not allowed." >&2
  exit 2
fi

for command in az kubectl helm; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Missing required command: ${command}" >&2
    exit 2
  }
done

current_context="$(kubectl config current-context)"
if [[ -n "${AKS_CLUSTER_NAME:-}" && "${current_context}" != "${AKS_CLUSTER_NAME}" ]]; then
  echo "Current kubectl context '${current_context}' is not AKS_CLUSTER_NAME '${AKS_CLUSTER_NAME}'." >&2
  exit 2
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
namespace="team-drops"

helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo add jetstack https://charts.jetstack.io
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add grafana-community https://grafana-community.github.io/helm-charts
helm repo update

kubectl create namespace "${namespace}" --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.loadBalancerIP="${INGRESS_PUBLIC_IP}" \
  --set-string controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-resource-group"="${AZURE_NODE_RESOURCE_GROUP}" \
  --wait \
  --timeout 10m

helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --set crds.enabled=true \
  --wait \
  --timeout 10m

cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    email: ${LETSENCRYPT_EMAIL}
    server: https://acme-v02.api.letsencrypt.org/directory
    privateKeySecretRef:
      name: letsencrypt-prod-account-key
    solvers:
      - http01:
          ingress:
            ingressClassName: nginx
EOF

kubectl -n "${namespace}" create secret generic team-drops-monitoring-grafana-admin \
  --from-literal=admin-user=admin \
  --from-literal=admin-password="${GRAFANA_ADMIN_PASSWORD}" \
  --dry-run=client -o yaml | kubectl apply -f -

helm dependency build "${repo_root}/helm/team-drops-monitoring"

helm upgrade --install team-drops-monitoring "${repo_root}/helm/team-drops-monitoring" \
  --namespace "${namespace}" \
  --values "${repo_root}/helm/team-drops-monitoring/values-azure.yaml" \
  --wait \
  --timeout 15m

helm upgrade --install team-drops "${repo_root}/helm/team-drops" \
  --namespace "${namespace}" \
  --values "${repo_root}/helm/team-drops/values-azure.yaml" \
  --set-string image.registry="${ACR_LOGIN_SERVER}" \
  --set-string image.tag="${IMAGE_TAG}" \
  --set-string ingress.host="${APP_HOSTNAME}" \
  --set-string frontend.keycloakUrl="https://${APP_HOSTNAME}" \
  --set-string auth.issuerUri="https://${APP_HOSTNAME}/realms/team-drops" \
  --set-string keycloak.redirectUris[0]="https://${APP_HOSTNAME}/*" \
  --set-string keycloak.webOrigins[0]="https://${APP_HOSTNAME}" \
  --set-string genai.llmApiKey="${LLM_API_KEY}" \
  --set-string postgres.password="${POSTGRES_PASSWORD}" \
  --set-string keycloak.adminPassword="${KEYCLOAK_ADMIN_PASSWORD}" \
  --set-string auth.clientSecret="${KEYCLOAK_CLIENT_SECRET}" \
  --wait \
  --timeout 15m

kubectl -n "${namespace}" get pods,svc,ingress,pvc
echo "Azure deployment completed: https://${APP_HOSTNAME}"
