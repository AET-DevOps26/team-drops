#!/usr/bin/env bash
set -euo pipefail

: "${AZURE_RESOURCE_GROUP:?Set AZURE_RESOURCE_GROUP from Terraform output resource_group_name}"
: "${AKS_CLUSTER_NAME:?Set AKS_CLUSTER_NAME from Terraform output aks_cluster_name}"

command -v az >/dev/null 2>&1 || {
  echo "Missing required command: az" >&2
  exit 2
}

az aks get-credentials \
  --resource-group "${AZURE_RESOURCE_GROUP}" \
  --name "${AKS_CLUSTER_NAME}" \
  --overwrite-existing

kubectl config use-context "${AKS_CLUSTER_NAME}"
kubectl get nodes
