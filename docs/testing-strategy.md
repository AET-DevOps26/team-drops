# Testing Strategy and Coverage Summary

This document summarizes the automated test coverage currently in the repository and the purpose of each test type.

## Test Types Covered

| Type | Location | Command | What it validates |
| --- | --- | --- | --- |
| Frontend unit tests | `frontend/src/**/*.test.js` | `cd frontend && npm run test` | Pure data transformations such as mapping backend plans, lessons, answers, feedback, and progress into UI state. |
| Frontend integration tests | `frontend/src/**/*.test.js` | `cd frontend && npm run test` | Browser API-client behavior with mocked `fetch`, including headers, token refresh, JSON request bodies, backend validation errors, and network failures. |
| Frontend end-to-end tests | `frontend/e2e/*.spec.js` | `cd frontend && npm run test:e2e` | Real browser workflow through the Vite app: bypass authentication, load mocked backend data, open the current learning plan, open a lesson, and reach an exercise. |
| GenAI service tests | `genai/tests/*.py` | `cd genai && uv run pytest tests/ -v` | FastAPI route behavior, prompt/schema handling, RAG behavior, listening/speaking/writing/practice flows, LLM configuration, and authentication middleware. |
| Backend service unit tests | `backend/*-service/src/test/java/**` | `cd backend/<service> && ./gradlew test` | Spring service logic for users, learning plans, lessons, exercises, progress records, answers, and generated feedback integrations. |
| Backend application context tests | `backend/*-service/src/test/java/**ApplicationTests.java` | `cd backend/<service> && ./gradlew test` | Spring Boot application wiring and configuration sanity checks. |
| CI-enforced tests | `.github/workflows/*.yml` | GitHub Actions | GenAI tests, frontend unit/integration/E2E tests, frontend build, backend checks, Docker image builds, OpenAPI export validation, and OpenAPI linting. |

## Recently Added Coverage

- Added frontend unit tests for mapper behavior, including fallback lesson blocks, saved answers, feedback mapping, lesson progress, plan progress, and dashboard progress summaries.
- Added frontend API integration tests for authenticated request headers, refreshed tokens, encoded query parameters, JSON body serialization, backend error payloads, and network availability failures.
- Added Playwright end-to-end coverage for the critical learner path from authentication bypass to dashboard, learning plan, lesson detail, and exercise detail.
- Updated frontend CI so unit, integration, and end-to-end tests run automatically before the frontend build.

## Critical Flows Covered

- Learner enters the app without Keycloak in local/no-auth mode.
- Dashboard renders user profile and active learning plan data from backend service responses.
- Learner opens the current learning plan and selects a lesson.
- Lesson content and exercises render from normalized backend data.
- API failures are converted into user-facing service availability errors instead of leaking raw fetch errors.
- Progress and feedback state are derived from completed answers and remain consistent with dashboard summaries.

## Remaining Useful Additions

- Backend controller-level integration tests with MockMvc for authenticated request/response contracts.
- Contract tests that compare frontend client expectations against the OpenAPI service specs.
- Cross-service Docker Compose smoke tests for the full microservice stack.
- Negative end-to-end browser tests for backend outages and empty learning-plan states.
