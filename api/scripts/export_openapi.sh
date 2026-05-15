#!/usr/bin/env bash
# Exports the FastAPI OpenAPI spec to api/openapi.yaml
# Run from the repo root: bash api/scripts/export_openapi.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT="${REPO_ROOT}/api/openapi.yaml"

cd "${REPO_ROOT}/genai"

LLM_PROVIDER=ollama uv run python -c "
import yaml, os
os.environ.setdefault('LLM_PROVIDER', 'ollama')
from app.main import app
with open('${OUTPUT}', 'w') as f:
    yaml.dump(app.openapi(), f, allow_unicode=True, sort_keys=False)
print('Exported OpenAPI spec to ${OUTPUT}')
"
