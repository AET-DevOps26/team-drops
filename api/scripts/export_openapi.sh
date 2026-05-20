#!/usr/bin/env bash
# Generates api/services/genai.yaml from FastAPI and rebuilds api/openapi.yaml.
# Run from the repo root: bash api/scripts/export_openapi.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
mkdir -p "${REPO_ROOT}/api/services"

cd "${REPO_ROOT}/genai"

GENAI_SPEC_PATH="${REPO_ROOT}/api/services/genai.yaml"
if command -v cygpath >/dev/null 2>&1; then
  GENAI_SPEC_PATH="$(cygpath -w "${GENAI_SPEC_PATH}")"
fi
export GENAI_SPEC_PATH

PYTHONUTF8=1 LLM_PROVIDER=ollama uv run python -c "
import yaml, os
os.environ.setdefault('LLM_PROVIDER', 'ollama')
from app.main import app
with open(os.environ['GENAI_SPEC_PATH'], 'w', encoding='utf-8') as f:
    yaml.dump(app.openapi(), f, allow_unicode=True, sort_keys=False)
print('Exported to api/services/genai.yaml')
"

npx --yes @redocly/cli join \
  "${REPO_ROOT}/api/base.yaml" \
  "${REPO_ROOT}/api/services/"*.yaml \
  -o "${REPO_ROOT}/api/openapi.yaml" \
  --without-x-tag-groups

echo "Joined specs into api/openapi.yaml"

npx --yes @redocly/cli lint --config "${REPO_ROOT}/api/redocly.yaml" "${REPO_ROOT}/api/openapi.yaml"