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

## CI/CD

The repository includes GitHub Actions workflows for continuous integration.

Workflow files:

```text
.github/workflows/backend-ci.yml   # Builds all three Spring Boot services and their Docker images
.github/workflows/genai-ci.yml     # Builds the GenAI service Docker image