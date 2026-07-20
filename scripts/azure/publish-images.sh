#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ACR_LOGIN_SERVER=<name>.azurecr.io IMAGE_TAG=sha-<commit> publish-images.sh --confirm

Builds and pushes the five Team Drops linux/amd64 images to Azure Container
Registry. Nothing happens unless --confirm is supplied.
EOF
}

if [[ "${1:-}" != "--confirm" ]]; then
  usage
  exit 2
fi

: "${ACR_LOGIN_SERVER:?Set ACR_LOGIN_SERVER from terraform output acr_login_server}"
: "${IMAGE_TAG:?Set IMAGE_TAG to an immutable tag such as sha-<commit>}"

if [[ "${IMAGE_TAG}" == "latest" ]]; then
  echo "IMAGE_TAG must be immutable; latest is not allowed." >&2
  exit 2
fi

for command in az docker; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Missing required command: ${command}" >&2
    exit 2
  }
done

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
acr_name="${ACR_LOGIN_SERVER%%.*}"

az acr login --name "${acr_name}"

build_image() {
  local image_name="$1"
  local context="$2"
  local dockerfile="$3"

  docker buildx build \
    --platform linux/amd64 \
    --file "${repo_root}/${dockerfile}" \
    --tag "${ACR_LOGIN_SERVER}/team-drops/${image_name}:${IMAGE_TAG}" \
    --push \
    "${repo_root}/${context}"
}

build_image team-drops-frontend frontend frontend/Dockerfile
build_image team-drops-user-service . backend/user-service/Dockerfile
build_image team-drops-learning-service . backend/learning-service/Dockerfile
build_image team-drops-progress-feedback-service . backend/progress-feedback-service/Dockerfile
build_image team-drops-genai-service . genai/Dockerfile

echo "Published Team Drops images to ${ACR_LOGIN_SERVER} with tag ${IMAGE_TAG}."
