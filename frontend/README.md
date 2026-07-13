# Team Drops Frontend

The Team Drops frontend is a React 19 single-page application built with Vite
6. It provides authentication, language selection, learning-plan navigation,
exercise submission, feedback, progress views, and profile management.

The production image builds the application into static assets and serves them
through Nginx. The project includes a TypeScript toolchain, while the current UI
implementation is primarily JSX and JavaScript.

## Requirements

- Node.js 20, matching the frontend CI workflow
- npm
- The user, learning, and progress services when testing API-backed features
- Keycloak when authentication is enabled

## Install and run

```bash
npm ci
npm run dev
```

The development server listens on <http://localhost:3000> and proxies relative
service paths to these defaults:

| Browser path | Default target |
| --- | --- |
| `/user-service` | `http://127.0.0.1:8081` |
| `/learning-service` | `http://127.0.0.1:8082` |
| `/progress-service` | `http://127.0.0.1:8083` |

To override proxy targets, provide `VITE_USER_SERVICE_URL`,
`VITE_LEARNING_SERVICE_URL`, or `VITE_PROGRESS_SERVICE_URL` in the shell that
starts Vite. Running the complete repository with Docker Compose configures the
service routing automatically.

## Authentication configuration

For direct frontend development, create `frontend/.env.local` when the defaults
are not suitable:

```dotenv
VITE_AUTH_ENABLED=true
VITE_KEYCLOAK_URL=http://localhost:8090
VITE_KEYCLOAK_REALM=team-drops
VITE_KEYCLOAK_CLIENT_ID=team-drops
```

Authentication is disabled by the frontend when `VITE_AUTH_ENABLED` is absent
or set to `false`. When enabled, start Keycloak and the protected APIs with
compatible issuer and realm settings. Runtime deployment values can also be
provided through `window.__APP_CONFIG__`; they take precedence over Vite build
variables.

## Quality checks

```bash
npm run lint
npm run build
npm run preview
```

`npm run build` runs the TypeScript project build before producing the Vite
bundle. The frontend CI workflow also builds the production Docker image.

## Source layout

```text
src/
|-- api/          # Relative-path API client and response mappers
|-- auth/         # Keycloak initialization and token handling
|-- components/   # Shared UI components
|-- data/         # Language and learning-plan presentation data
|-- pages/        # Main application screens
`-- utils/        # Learning-related helpers
```

The browser must use relative service paths instead of Docker-only hostnames.
Vite handles these paths locally, and Nginx or Kubernetes Ingress handles them
after deployment.

## API contract changes

The source of truth is [`../api/openapi.yaml`](../api/openapi.yaml). The
frontend does not currently commit a generated TypeScript API client. When the
contract changes, update `src/api/client.js` and `src/api/mappers.js` together
with the affected UI behavior. Do not introduce duplicate request or response
shapes that conflict with the OpenAPI contract.

Export the central and service contracts from the repository root before
committing an API change:

```bash
bash api/scripts/export_openapi.sh
```

See the repository [API-first workflow](../README.md#api-first-workflow) and
[frontend/backend integration requirements](../docs/frontend-backend-api-integration.md)
for additional guidance.
