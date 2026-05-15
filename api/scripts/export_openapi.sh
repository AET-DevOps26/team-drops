#!/usr/bin/env bash
# Generates api/services/genai.yaml from FastAPI and rebuilds api/openapi.yaml.
# Run from the repo root: bash api/scripts/export_openapi.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
mkdir -p "${REPO_ROOT}/api/services"

cd "${REPO_ROOT}/genai"
LLM_PROVIDER=ollama uv run python -c "
import yaml, os
os.environ.setdefault('LLM_PROVIDER', 'ollama')
from app.main import app
with open('${REPO_ROOT}/api/services/genai.yaml', 'w') as f:
    yaml.dump(app.openapi(), f, allow_unicode=True, sort_keys=False)
print('Exported to api/services/genai.yaml')
"

npx --yes @redocly/cli join \
  "${REPO_ROOT}/api/base.yaml" \
  "${REPO_ROOT}/api/services/"*.yaml \
  -o "${REPO_ROOT}/api/openapi.yaml" \
  --without-x-tag-groups
echo "Joined specs into api/openapi.yaml"

npx --yes @redocly/cli lint --config "${REPO_ROOT}/redocly.yaml" "${REPO_ROOT}/api/openapi.yaml"
