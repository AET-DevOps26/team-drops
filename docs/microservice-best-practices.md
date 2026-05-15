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
└── api/
    ├── openapi.yaml          # The spec — edit this, not generated files
    ├── .redocly.yaml         # Linting config
    └── scripts/
        └── export_openapi.sh # Re-export from FastAPI after schema changes
```

### Always version your APIs

All routes must use a version prefix from day one:

```
/api/v1/exercises/generate
/api/v1/writing/evaluate
```

Never expose unversioned routes. This prevents breaking clients during iteration.

### Code generation

| Task | Command |
|------|---------|
| Lint spec | `npx @redocly/cli lint api/openapi.yaml` |
| Export from FastAPI | `bash api/scripts/export_openapi.sh` |
| Generate Java stubs | `openapi-generator-cli generate -i api/openapi.yaml -g spring -o services/spring-order/generated` |
| Generate Python client | `openapi-python-client --path api/openapi.yaml --output services/py-recommender/client` |
| Generate TypeScript SDK | `npx openapi-typescript api/openapi.yaml -o web-client/src/api.ts` |
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
      args: [api/openapi.yaml]
```

---

## 3. Security

- Use OAuth2 / OIDC via an API gateway (e.g. Keycloak, Auth0). Pass JWTs in HTTP headers.
- Each service must verify the JWT using a shared public key — never trust caller identity without verification.
- Place the gateway as the single ingress point to centralise token validation before forwarding to services.

---

## 4. Development & Deployment

### Contract testing

Use [Pact](https://docs.pact.io) to verify API contract fidelity between producer (Spring Boot) and consumer (Python/TypeScript). A passing unit test suite does not prove the contract is correct.

### Service discovery

Route all traffic through an API gateway (Traefik, NGINX). Avoid direct service-to-service calls across language boundaries unless they go through a generated client.

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

1. OpenAPI spec lint (`npx @redocly/cli lint api/openapi.yaml`)
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

- Import the generated `api.ts` SDK in React/TypeScript — never hand-write HTTP calls.
- Set the base URL from an environment variable: `VITE_API_URL=http://localhost:8080`
- Use [SWR](https://swr.vercel.app) or [React Query](https://tanstack.com/query) for caching and retries.
- Configure CORS at the gateway level, not per-service.

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
| No direct HTTP calls without a generated client | Hidden formats drift and break silently |
| No shared DTOs or utilities outside the OpenAPI spec | Creates coupling between services |
| No long-running branches (> 2 days) | Merge conflicts compound; rebase regularly |
| No manual production deploys | Untraceable, unreproducible, risky |
| No skipping the OpenAPI linter (`--no-verify`) | The spec is the contract — a broken spec means broken clients |
