# Team Drops

Team Drops is an AI-assisted language-learning platform. It combines structured
learning plans and exercises with generated content, speaking and listening
practice, automated feedback, progress tracking, and Keycloak-based
authentication.

The repository contains the browser application, three Spring Boot domain
services, a FastAPI GenAI service, local Docker infrastructure, Kubernetes Helm
charts, and the project documentation.

## Project status

The repository represents the final integrated project architecture. The local
stack runs through Docker Compose, and the Rancher deployment uses separate
application and monitoring releases. `api/openapi.yaml`, the Helm values, and
the GitHub Actions workflows are the sources of truth for runtime behavior;
older proposal and diagram files are retained as historical design material.

## Capabilities

- User registration, authentication, and profile management
- Language-specific learning plans, lessons, and exercises
- Answer submission, feedback, and progress tracking
- LLM-assisted content generation and retrieval-augmented generation
- Speech-to-text and text-to-speech exercises
- Application metrics, version visibility, dashboards, alerting, and logs

## Architecture

| Component | Responsibility | Technology | Local port | Main dependencies |
| --- | --- | --- | ---: | --- |
| Frontend | Learning interface and authentication flow | React 19, Vite 6, TypeScript 5 toolchain, Nginx | 3000 | Keycloak and the three domain APIs |
| User service | Users and profiles | Spring Boot 3.5, Java 21 | 8081 | PostgreSQL, Keycloak |
| Learning service | Learning plans, lessons, and exercises | Spring Boot 3.5, Java 21 | 8082 | PostgreSQL, Keycloak |
| Progress and feedback service | Answers, generated feedback, and progress | Spring Boot 3.5, Java 21 | 8083 | PostgreSQL, domain services, GenAI service |
| GenAI service | Generation, RAG, speech-to-text, and text-to-speech | Python 3.13, FastAPI | 8084 | Ollama or OpenAI-compatible API, MongoDB, Keycloak |
| Keycloak | Identity and access management | Keycloak 26 | 8090 | Imported `team-drops` realm |
| PostgreSQL | Relational application data | PostgreSQL 16 | 5433 | Backend services |
| MongoDB | GenAI and retrieval data | MongoDB 7 | 27017 | GenAI service |
| Ollama | Local language-model runtime | Ollama | 11434 | GenAI service |

Browser requests use relative paths such as `/user-service`,
`/learning-service`, and `/progress-service`. Vite proxies these paths during
local development, while Nginx and Kubernetes Ingress route them in deployed
environments.

## Repository layout

```text
team-drops/
|-- api/                         # Central and per-service OpenAPI contracts
|-- backend/
|   |-- user-service/
|   |-- learning-service/
|   `-- progress-feedback-service/
|-- frontend/                    # React browser application
|-- genai/                       # FastAPI GenAI service
|-- helm/
|   |-- team-drops/              # Application Helm chart and operations guide
|   `-- team-drops-monitoring/   # Prometheus, Grafana, Alertmanager, Loki, Alloy
|-- keycloak/                    # Local realm export
|-- docs/                        # Architecture and project documentation
|-- .env.example                 # Local configuration example
`-- docker-compose.yml           # Complete local stack
```

## Quick start with Docker Compose

Requirements:

- Docker with Docker Compose v2
- Enough memory and disk space to run the services and download the configured
  Ollama model

Create your local configuration and start the stack:

```bash
cp .env.example .env
docker compose up --build
```

On Windows PowerShell, use `Copy-Item .env.example .env` for the first command.
The initial start may take longer while Ollama downloads
`OLLAMA_MODEL` (`llama3.2:1b` by default).

Useful local endpoints:

| Endpoint | URL |
| --- | --- |
| Frontend | <http://localhost:3000> |
| Keycloak | <http://localhost:8090> |
| User health | <http://localhost:8081/actuator/health> |
| Learning health | <http://localhost:8082/actuator/health> |
| Progress health | <http://localhost:8083/actuator/health> |
| GenAI health | <http://localhost:8084/health> |

Stop the stack with `docker compose down`. Add `--volumes` only when you
intentionally want to remove the local PostgreSQL, MongoDB, and Ollama data.

## Configuration and authentication

`.env.example` documents the supported local LLM and authentication settings.
The default example uses Ollama. To use an OpenAI-compatible provider, set
`LLM_PROVIDER`, `LLM_API_KEY`, `LLM_MODEL`, and, when required,
`LLM_BASE_URL`. Never commit real credentials or add them to Helm values.

Authentication is controlled by `AUTH_ENABLED`. When enabled, the frontend and
APIs use the `team-drops` Keycloak realm and client. The local realm is imported
from `keycloak/realm-export.json`. See
[Keycloak authentication](docs/keycloak-authentication.md) for the complete
flow and configuration.

## Development and testing

### Backend services

Each backend service has an independent Gradle wrapper. From a service
directory, run:

```bash
./gradlew build
```

Use `gradlew.bat build` on Windows. The services require Java 21.

### Frontend

```bash
cd frontend
npm ci
npm run dev
npm run lint
npm run build
```

See the [frontend guide](frontend/README.md) for proxy and Keycloak settings.

### GenAI service

The GenAI project uses `uv` and Python 3.13 or newer:

```bash
cd genai
uv sync --group dev
uv run pytest tests/ -v
```

## API-first workflow

`api/openapi.yaml` is the central API contract. Design API changes there first,
then update the implementation and derived service contracts. Export and check
the contracts with:

```bash
bash api/scripts/export_openapi.sh
```

On Windows PowerShell with Git Bash installed:

```powershell
& "C:\Program Files\Git\bin\bash.exe" api/scripts/export_openapi.sh
```

Commit changes to the central contract and affected files under
`api/services/`. Generated backend interfaces and models are managed by each
service's Gradle OpenAPI task; do not edit generated output manually. The
frontend currently keeps its request layer in `frontend/src/api/client.js` and
response mapping in `frontend/src/api/mappers.js`, so contract changes must be
reflected there as well. CI checks that exported OpenAPI specifications remain
in sync.

## Kubernetes deployment

Kubernetes uses two independently managed Helm releases in the existing
`team-drops` Rancher namespace:

| Release | Chart | Namespace |
| --- | --- | --- |
| `team-drops` | `helm/team-drops` | `team-drops` |
| `team-drops-monitoring` | `helm/team-drops-monitoring` | `team-drops` |

The deployment workflow does not create or modify the Namespace object. Create
`team-drops` through Rancher with an authorized identity before the first
deployment. Application and monitoring credentials are supplied through GitHub
repository secrets and namespaced Kubernetes Secrets, never committed values.

See the [Helm deployment and operations guide](helm/team-drops/README.md) for
required secrets, rendering, installation, verification, rollback, and
troubleshooting commands.

## Observability

The separately owned `team-drops-monitoring` release provides:

- Prometheus with seven-day retention and a persistent 2 GiB volume
- Grafana with provisioned Prometheus and Loki datasources and persistent
  configuration
- A 13-panel **Team Drops Overview** dashboard for traffic, errors, latency,
  availability, versions, replicas, restarts, JVM memory, readiness, scrape
  duration, and active alerts
- A **Team Drops Logs** dashboard for log volume, errors, and container streams
- Loki with three-day retention on a persistent 2 GiB volume
- Alloy collecting logs only from the `team-drops` namespace
- kube-state-metrics collecting only pod and deployment state from
  `team-drops`, using namespaced read-only RBAC
- Alertmanager routing warnings to enabled Slack and email receivers and
  critical alerts to enabled Slack, email, and PagerDuty receivers
- Four runtime alerts: service down, high error rate, slow responses, and pod
  restart bursts

The monitoring chart does not manage ResourceQuota or LimitRange because those
policies apply to the shared namespace and remain under Rancher administration.
The deployment also excludes node-exporter, privileged host access, and node
infrastructure dashboards.

## CI/CD

| Workflow | Purpose |
| --- | --- |
| `backend-ci.yml` | Lints the OpenAPI contract and builds the three Java services and Docker images |
| `frontend-ci.yml` | Installs dependencies and builds the frontend and its Docker image |
| `genai-ci.yml` | Runs GenAI tests, verifies exported API contracts, and builds its Docker image |
| `docker-publish.yml` | Builds and publishes application images to GHCR |
| `deploy-kubernetes.yml` | Validates both Helm charts and deploys monitoring followed by the application |

The CI workflows run on relevant pull requests and pushes. Kubernetes
deployment runs after a successful main-branch image publication and can also
be started manually.

## Documentation

The [documentation index](docs/README.md) groups the architecture, API,
authentication, deployment, and historical project material. Key references:

- [Frontend/backend API integration](docs/frontend-backend-api-integration.md)
- [Keycloak authentication](docs/keycloak-authentication.md)
- [Nginx gateway](docs/nginx-gateway.md)
- [Microservice best practices](docs/microservice-best-practices.md)
- [Helm deployment and operations](helm/team-drops/README.md)

Historical planning material, including the proposal, diagrams, and
[product backlog](https://github.com/AET-DevOps26/team-drops/wiki/Product-Backlog),
remains available for project context.
