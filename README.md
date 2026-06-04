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
- [Backend API Requirements](docs/frontend-backend-api-integration.md)

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
├── frontend/                       # React + TypeScript (Vite) — client UI, served by Nginx in Kubernetes
├── genai/                          # Python + FastAPI — LLM-powered generation and feedback (port 8084)
├── helm/team-drops/                # Helm chart for Rancher/Kubernetes deployment
├── shared/                         # Shared TypeScript types mirroring the backend domain model
├── docs/                           # Architecture diagrams and project proposal
├── .env.example                    # Environment variable reference
└── docker-compose.yml              # Local development setup
```
## API Workflow

Main rule:

```text
Update api/openapi.yaml first
→ generate code/types
→ implement business logic
```

API changes should be designed and reviewed before implementation.

Use versioned API paths from the beginning, for example:

```text
/api/v1/users
/api/v1/learning-plans
/api/v1/progress
```


Export the central OpenAPI contract:

macOS/Linux:

```bash
bash api/scripts/export_openapi.sh
```

Windows PowerShell, using Git Bash explicitly:

```powershell
& "C:\Program Files\Git\bin\bash.exe" api/scripts/export_openapi.sh
```

Then commit the updated OpenAPI files:

```bash
git add api/openapi.yaml api/services/*.yaml
```

CI fails if the committed OpenAPI files are out of sync with the code or generated specs.

Do not manually create duplicate DTOs or API request/response types.  
Do not edit generated files manually.  
If generated code is wrong, fix `api/openapi.yaml` and regenerate.

---

## Backend

Backend services use **OpenAPI Generator** with the Spring generator.

The generator creates:

- API interfaces from OpenAPI paths
- DTO/model classes from OpenAPI schemas

The application code implements the generated interfaces and keeps business logic in the service layer.

Generate Java stubs:

```bash
openapi-generator-cli generate -i api/openapi.yaml -g spring -o backend/<service-name>/generated
```

If configured through Gradle:

```bash
./gradlew :<service-name>:openApiGenerate
```

Windows:

```powershell
.\gradlew.bat :<service-name>:openApiGenerate
```

Project versions:

```text
Spring Boot 4.x
Java 25 LTS
```

---

## Frontend

Generate TypeScript API types from the OpenAPI spec:

```bash
npx openapi-typescript api/openapi.yaml -o frontend/src/api/types.ts
```

Do not hand-write request or response types.

### Frontend (TODO)

## Mock Server

For frontend development before backend implementation is ready:

```bash
npx prism mock api/openapi.yaml
```

---

## Local Development

```bash
docker compose up --build
```

## Kubernetes Deployment

The project is deployed to Rancher/Kubernetes with Helm. The chart lives in
`helm/team-drops` and does not hardcode a namespace; pass the target namespace on
the Helm command line.

Render and validate without changing the cluster:

```bash
helm lint ./helm/team-drops \
  --set ingress.host=test.stud.k8s.aet.cit.tum.de \
  --set genai.llmApiKey=dummy

helm template team-drops ./helm/team-drops \
  --namespace team-drops \
  --set ingress.host=test.stud.k8s.aet.cit.tum.de \
  --set genai.llmApiKey=dummy \
  | kubectl apply --dry-run=server -n team-drops -f -
```

Install or update the application:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  --set ingress.host=team-drops.stud.k8s.aet.cit.tum.de \
  --set genai.llmApiKey=<api-key>
```

For testing images built from the `kubernetes` branch before merging to `main`,
add:

```bash
--set image.tag=kubernetes
```

After deployment, the ingress host exposes:

```text
/                         frontend
/user-service/...          user-service
/learning-service/...      learning-service
/progress-service/...      progress-feedback-service
/api/v1/genai/...          genai-service
```

See `helm/team-drops/README.md` for validation, private image pull secrets,
GenAI configuration, and troubleshooting commands.

## CI/CD

The repository includes GitHub Actions workflows for continuous integration.

Workflow files:

```text
.github/workflows/backend-ci.yml      # Builds all three Spring Boot services and their Docker images
.github/workflows/genai-ci.yml        # Tests and builds the GenAI service Docker image
.github/workflows/docker-publish.yml  # Builds all service images and publishes them to GHCR on main/tags/manual runs
```
