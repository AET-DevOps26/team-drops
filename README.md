# team-drops

Repository for team DrOps.

## Documentation

The project documentation is stored in the `docs` folder.

- [Project Proposal](docs/Project%20Proposal.pdf)
- [System Structure](docs/system%20structure.pdf)
- [Class Diagram](docs/Class%20Diagram.pdf)
- [Use Case Diagram](docs/Use%20Case%20Diagram.pdf)
- [Top-Level Architecture Diagram](docs/top-level%20architecture%20diagram.pdf)
- [Microservice Best Practices](docs/microservice-best-practices.md)

## Product Backlog

The first product backlog is available in the GitHub Wiki:

- [Product Backlog](../../wiki/Product-Backlog)
- [Product Backlog: User Stories](../../wiki/Product-Backlog:-User-Stories)

## Project Structure

```text
team-drops/
├── backend/
│   ├── user-service/               # Spring Boot — user registration, login, profiles (port 8081)
│   ├── learning-service/           # Spring Boot — learning plans, lessons, exercises (port 8082)
│   └── progress-feedback-service/  # Spring Boot — answers, feedback, progress (port 8083)
├── frontend/                       # React + TypeScript (Vite) — client UI (port 3000)
├── genai/                          # Python + FastAPI — LLM-powered generation and feedback (port 8084)
├── shared/                         # Shared TypeScript types mirroring the backend domain model
├── docs/                           # Architecture diagrams and project proposal
├── .env.example                    # Environment variable reference
└── docker-compose.yml              # Local development setup
```

## API Contract

The central API contract lives in `api/openapi.yaml`. It is **generated — do not edit it by hand**.

Each service owns a spec file in `api/services/<service-name>.yaml` (auto-generated from the service's code and committed). The central spec is produced by joining `api/base.yaml` (platform-level info) with all service specs via `redocly join`.

### Working on the GenAI service

After changing a FastAPI schema or route, regenerate and commit both updated files:

```bash
bash api/scripts/export_openapi.sh
git add api/services/genai.yaml api/openapi.yaml
```

CI fails with a clear message if either committed file is out of sync with the code.

### Backend services (TODO)

Each Spring Boot service needs to plug into this workflow:

1. Add `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9` to `build.gradle`
2. Add `org.springdoc.openapi-gradle-plugin:1.9.0`, configured to output to `api/services/<service-name>.yaml`
3. Run `./gradlew :<service>:generateOpenApiDocs` after changing a controller or DTO and commit the result
4. Add the new spec file to the `redocly join` call in `api/scripts/export_openapi.sh` and register it in `api/redocly.yaml`

### Frontend (TODO)

Once the central spec is populated with all services:

1. Add `openapi-typescript` as a dev dependency
2. Add to `package.json`: `"generate:api": "openapi-typescript ../../api/openapi.yaml -o src/api/types.ts"`
3. Run `npm run generate:api` whenever the central spec changes and commit `src/api/types.ts`
4. Import from `src/api/types.ts` — never hand-write HTTP request/response types

---

## CI/CD

The repository includes GitHub Actions workflows for continuous integration.

Workflow files:

```text
.github/workflows/backend-ci.yml   # Builds all three Spring Boot services and their Docker images
.github/workflows/genai-ci.yml     # Builds the GenAI service Docker image