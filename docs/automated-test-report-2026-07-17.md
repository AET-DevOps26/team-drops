# Automated Test Report - 2026-07-17

## Scope

This report records the local verification baseline for the backend and GenAI
changes. External LLM, STT, and TTS providers were replaced with deterministic
test doubles. Backend API integration tests used random local HTTP ports and
isolated in-memory H2 databases.

Service E2E tests additionally use real PostgreSQL 16 Testcontainers. Docker was
not running on the local verification machine, so the three Java E2E classes
were compiled successfully and confirmed to skip cleanly. They execute on the
Docker-capable manual GitHub Actions runner, where a `docker info` gate prevents
silent skipping. The GenAI service E2E test does not require Docker and was
executed locally.

## Results

| Suite | Command | Result |
| --- | --- | ---: |
| User service | `gradlew.bat test` | 11 passed |
| Learning service | `gradlew.bat test` | 51 passed |
| Progress-feedback service | `gradlew.bat test` | 40 passed, 1 live-provider manual test skipped |
| Backend PostgreSQL service E2E | `gradlew.bat test --tests *E2ETests` | 3 compiled, 3 skipped locally because Docker was unavailable |
| GenAI service | `uv run pytest tests/ -v --junitxml=test-results/junit.xml` | 89 passed, including 1 service E2E |
| GenAI RAG test lint | `uv run ruff check tests/test_rag_engine.py` | Passed |
| Backend packaging | `gradlew.bat build` for all three services | Passed |

No automated suite reported a failure or error.

## Test types

| Type | Coverage in this report |
| --- | --- |
| Unit | Business rules, validation, normalization, score calculation, and schema repair |
| Client integration | Outbound service clients, authentication headers, payloads, and downstream error mapping |
| API integration | Real Spring/FastAPI entrypoints with H2 or deterministic provider fakes |
| Service E2E | Real public API through the assembled service and PostgreSQL, or FastAPI through the real BM25 pipeline |
| Browser E2E | Reported separately by Frontend CI |
| Full-system E2E | Not implemented; see the testing strategy's known boundary |

## Covered backend behaviour

- User creation/reuse, profile defaults, authentication boundaries, public
  metrics, and real `/api/v1/users/me` HTTP/persistence behaviour.
- Fixed and RAG learning-plan validation, lesson/exercise persistence,
  localization, downstream client failures, authentication boundaries, public
  metrics, and real plan-catalog HTTP/persistence behaviour.
- Writing, listening, and speaking submissions; feedback/progress persistence;
  negative validation; downstream failures; authentication boundaries; public
  metrics; and real empty-progress HTTP/persistence behaviour.
- PostgreSQL-backed service E2E paths for user creation/readback, learning-plan
  catalog seeding/reuse, and scoped progress retrieval.

## Covered GenAI behaviour

- FastAPI routing, middleware, request validation, error responses, health,
  metrics, and bearer authentication.
- Writing, speaking, listening, practice, and exercise-generation success and
  failure paths.
- RAG corpus routes, source grounding, exercise-type validation, interview
  relevance review, corrective regeneration, and final quality failure.
- Real local BM25 retrieval ranking, source metadata, empty corpora, missing
  topics, topic discovery, and path-traversal rejection.
- A GenAI service E2E request through FastAPI, real retrieval, controlled LLM
  orchestration, and public response mapping.

See [Testing Strategy and Coverage](testing-strategy.md) for the complete
behaviour-to-test mapping.

## CI execution policy

Backend and GenAI tests are temporarily manual to avoid rerunning the complete
suites on every pull-request update. They can be started from the GitHub Actions
page with **Run workflow**:

- **Backend CI** runs `test-backend-services` and uploads
  `backend-test-reports`.
- **GenAI CI** runs `test` and uploads `genai-test-report`.

Automatic pull-request jobs still compile/package the backend without tests,
validate API contracts, and build images. Full test suites should be run
manually again before merging.
