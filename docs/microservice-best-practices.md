# Microservice Best Practices

Guidelines for this repository. All services and PRs must follow these rules.

---

## 1. Microservice Architecture & Design

- **Single Responsibility** — each service owns exactly one domain or business capability. No overlapping responsibilities.
- **Stateless services** — no in-process session state. Use JWTs for auth context; use Redis or a shared DB if cross-request state is truly needed.
- **Language boundary awareness** — define the contract (OpenAPI spec) between Spring Boot (Java) and Python services explicitly. Communicate over JSON/HTTP, never via shared data structures.

---

## 2. API-First Design (most important rule)

### Design before you implement

Teams must define and review the OpenAPI spec collaboratively before writing any implementation logic. Use [Swagger Editor](https://editor.swagger.io) or [Stoplight](https://stoplight.io).

### Single source of truth

```
repo/
`-- api/
    |-- openapi.yaml          # Central contract; edit this first
    |-- redocly.yaml          # Linting configuration
    |-- services/             # Service-specific contracts
    `-- scripts/
        `-- export_openapi.sh # Export and rebuild synchronized contracts
```

### Always version your APIs

All routes must use a version prefix from day one:

```
/api/v1/users/me
/api/v1/genai/writing/evaluate
```

Never expose unversioned routes. This prevents breaking clients during iteration.

### Code generation

| Task | Command |
|------|---------|
| Lint spec | `npx --yes @redocly/cli lint --config api/redocly.yaml api/openapi.yaml` |
| Export from FastAPI | `bash api/scripts/export_openapi.sh` |
| Generate backend interfaces | Run `./gradlew openApiGenerate` inside the affected `backend/<service>` directory |
| Update frontend integration | Align `frontend/src/api/client.js` and `mappers.js`; no generated frontend client is currently committed |
| Mock server for client dev | `npx prism mock api/openapi.yaml` (runs on port 4010) |

### Never merge without linting the spec

Add to `.pre-commit-config.yaml` (already configured in this repo):

```yaml
- repo: local
  hooks:
    - id: redocly-lint
      name: Redocly OpenAPI lint
      language: node
      entry: npx --yes @redocly/cli lint
      args: [--config, api/redocly.yaml, api/openapi.yaml]
      pass_filenames: false
```

---

## 3. Security

- Use OAuth2/OIDC through the deployed Keycloak realm. Pass JWTs in HTTP headers.
- Each protected service verifies the JWT against the configured issuer and
  JWKS endpoint; never trust caller identity without verification.
- Nginx Ingress is the public entry point. In the current Rancher profile,
  services validate tokens and `oauth2-proxy` is disabled.

---

## 4. Development & Deployment

### Contract testing

CI lints the central OpenAPI file and checks exported contracts for drift. Pair
those checks with service and frontend tests whenever request or response
behavior changes; a passing unit test suite alone does not prove contract
compatibility.

### Service discovery

Browser traffic uses Nginx gateway prefixes. Internal orchestration uses stable
Kubernetes Service DNS names and contract-aligned HTTP clients. Do not expose
Docker-only hostnames to browser code.

### Consistent error schema

All services must return errors in this shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [...]
}
```

Define this schema once in `api/openapi.yaml` and enforce it everywhere.

### CI/CD pipeline requirements

Every PR must pass:

1. OpenAPI spec lint using `api/redocly.yaml`
2. Unit + integration tests
3. Docker image build

Merges to `main` additionally:

4. Push image to GHCR with `sha-<commit>` and `latest` tags

See `.github/workflows/genai-ci.yml` as the reference implementation.

### Docker image tagging

Push to `ghcr.io/<org>/<service>:<tag>` with both:
- `sha-<git-commit-sha>` — for traceability
- `latest` — for convenience in local dev

---

## 5. Client Integration

- Keep browser requests in `frontend/src/api/client.js` and response adaptation
  in `frontend/src/api/mappers.js` aligned with OpenAPI.
- Use relative `/user-service`, `/learning-service`, and `/progress-service`
  prefixes so Vite and Nginx can route the same client build.
- Forward the refreshed Keycloak bearer token for protected operations.
- Keep provider credentials and direct GenAI-provider calls out of the browser.

---

## 6. Collaboration Rules

| Practice | Expectation |
|----------|-------------|
| API review | Weekly 15-min sync to review `api/openapi.yaml` changes |
| Definition of Done | PR must include: passing CI, updated spec if endpoints changed, short doc entry if behaviour changed |
| Branch lifetime | Max 2 days — rebase or merge frequently. No long-running branches. |
| Production deploys | Everything must go through CI/CD. No manual deploys. |
| PR template | Check "Affects API?" and "Spec updated?" before requesting review |

---

## 7. Testing

- Write integration and E2E tests per microservice.
- Integration tests must hit a real database — do not mock the DB layer.
- Tests must cover: happy path, validation errors, downstream failures (e.g. LLM unreachable → 502 with correct error schema).
- Run tests locally before pushing: `cd genai && uv run pytest tests/ -v`

---

## 8. What Not To Do

| Rule | Why |
|------|-----|
| No HTTP integration that contradicts OpenAPI | Hidden formats drift and break silently |
| No shared DTOs or utilities outside the OpenAPI spec | Creates coupling between services |
| No long-running branches (> 2 days) | Merge conflicts compound; rebase regularly |
| No manual production deploys | Untraceable, unreproducible, risky |
| No skipping the OpenAPI linter (`--no-verify`) | The spec is the contract — a broken spec means broken clients |
