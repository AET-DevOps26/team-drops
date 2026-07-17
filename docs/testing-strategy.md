# Testing Strategy and Coverage

This document is the test inventory for InterviewMate. It explains which system
behaviours are protected, where those tests live, and which CI job prevents a
regression from being merged.

## Policy

Testing validates observable behaviour rather than implementation details.
Critical server-side and GenAI logic requires automated unit tests. Each backend
microservice also requires an integration test that crosses its real HTTP,
controller, service, serialization, and persistence layers, plus a service-level
E2E test against production-compatible infrastructure. Client-side tests cover
core interactions, and one Playwright scenario protects the primary learner
journey.

Frontend tests run automatically on pull requests to `main`. Backend and GenAI
tests are temporarily started manually with `workflow_dispatch` so repeated PR
updates do not rerun the full suites. Their automatic PR jobs still compile the
services and validate API contracts. Run both manual test jobs before merging.

| Aspect | Repository requirement | Enforcement |
| --- | --- | --- |
| Backend unit tests | Cover critical service rules, edge cases, and failures | Manual `Backend CI / test-backend-services` |
| Backend integration tests | Exercise every microservice through a real HTTP server and isolated H2 database | Manual `Backend CI / test-backend-services` |
| Backend service E2E tests | Exercise each public API through the assembled service and a real PostgreSQL container | Manual `Backend CI / test-backend-services` |
| Backend security/operations tests | Protect API authentication and public Prometheus endpoints | Manual `Backend CI / test-backend-services` |
| GenAI unit and API integration tests | Cover routes, schemas, prompts, provider failures, validation, authentication, RAG quality, and the local retrieval engine | Manual `GenAI CI / test` |
| Client tests | Cover API mapping, request failures, auth presentation, and the primary learner workflow | `Frontend CI / build-frontend` |
| Contract tests | Lint the combined OpenAPI contract and reject stale generated GenAI specifications | `Backend CI / openapi-lint` and `GenAI CI / sync-openapi` |
| Build verification | Compile/package applications and build deployable images | Backend, GenAI, and Frontend CI build jobs |

## Test type definitions

| Type | Boundary used in this repository |
| --- | --- |
| Unit | One class or function with collaborators mocked; no server or database |
| Client integration | A real outbound HTTP client against a local stub server, including serialization, authentication headers, and error mapping |
| API integration | The assembled Spring/FastAPI application through a real HTTP/ASGI entrypoint with deterministic in-memory infrastructure or provider fakes |
| Service E2E | The public service API through routing, security configuration, controllers/routers, business logic, serialization, and production-compatible persistence or the real internal RAG pipeline; only external systems outside that service are controlled |
| Browser E2E | A real Chromium user journey through the frontend; backend APIs are currently mocked |
| Full-system E2E | Frontend, Keycloak, databases, every microservice, and GenAI deployed together with no mocked service boundary; not implemented yet |

## Backend coverage

Backend tests use JUnit 5, AssertJ, Mockito, Spring Boot, H2, and Testcontainers.
Outbound client tests use local stub HTTP servers. API integration tests use
`@SpringBootTest(webEnvironment = RANDOM_PORT)` and `TestRestTemplate`, so they
exercise the application as a network client would without depending on shared
infrastructure. Service E2E tests use the same public HTTP boundary with a real
`postgres:16-alpine` container and are tagged `e2e`.

### User service

| Behaviour | Level | Automated coverage |
| --- | --- | --- |
| Create an OIDC user on first login; reuse by Keycloak subject; link an existing email | Unit | `UserServiceTests` |
| Create and consistently reuse the local development user | Unit + API integration | `UserServiceTests`, `UserServiceApiIntegrationTests` |
| Create default profiles and return existing profiles | Unit | `UserProfileServiceTests` |
| Persist and serialize `/api/v1/users/me` across two real HTTP calls | API integration | `UserServiceApiIntegrationTests` |
| Create and retrieve a persisted user through public HTTP and real PostgreSQL | Service E2E | `UserServiceE2ETests` |
| Reject unauthenticated API traffic when auth is enabled | Security integration | `UserServiceApplicationTests` |
| Start the Spring context and expose Prometheus metrics publicly | Application integration | `UserServiceApplicationTests` |

### Learning service

| Behaviour | Level | Automated coverage |
| --- | --- | --- |
| Create fixed and RAG plans; persist lessons, content blocks, and exercises | Unit | `LearningPlanServiceTests`, `LearningPlanSeederTests`, `LessonServiceTests` |
| Enforce RAG request bounds, lesson order, requested types, supported subtype pairs, and two-plan limit | Unit/negative | `LearningPlanServiceTests` |
| Avoid persistence when GenAI fails or returns malformed/unrequested content | Unit/failure | `LearningPlanServiceTests` |
| Seed and localize English/German interview catalogs and speaking tracks | Unit | `DefaultLearningPlanCatalogTests`, `LessonServiceTests` |
| Map exercise subtypes to reading, writing, listening, and speaking | Unit/edge | `ExerciseServiceTests` |
| Send the RAG request, propagate bearer tokens, parse valid responses, and map downstream/malformed failures | Client integration | `GenAiRagLearningPlanClientTests` |
| Resolve the authenticated user and reject missing tokens, mismatched users, and downstream failures | Client integration | `UserServiceClientTests` |
| Seed and return a complete plan catalog over a real HTTP/database path | API integration | `LearningServiceApiIntegrationTests` |
| Seed and reuse the plan catalog through public HTTP and real PostgreSQL | Service E2E | `LearningServiceE2ETests` |
| Reject unauthenticated API traffic when auth is enabled | Security integration | `LearningServiceApplicationTests` |
| Start the Spring context and expose Prometheus metrics publicly | Application integration | `LearningServiceApplicationTests` |

### Progress-feedback service

| Behaviour | Level | Automated coverage |
| --- | --- | --- |
| Submit writing/listening answers, persist feedback, and update scoped progress | Unit | `UserAnswerServiceTests`, `ProgressRecordServiceTests` |
| Evaluate speaking audio, localize exercise context, clamp scores, and allow absent feedback audio | Unit | `UserAnswerServiceTests` |
| Reject wrong users, missing fields, empty audio, wrong exercise type, blank transcription, missing exercises, and GenAI failures without partial persistence | Unit/negative | `UserAnswerServiceTests` |
| Aggregate progress by plan/language and return stable empty progress | Unit + API integration | `ProgressRecordServiceTests`, `ProgressFeedbackServiceApiIntegrationTests` |
| Send writing/listening/speaking GenAI requests with correct auth and multipart payloads | Client integration | `GenAiWritingClientTests`, `GenAiListeningClientTests`, `GenAiSpeakingClientTests` |
| Map downstream errors, missing auth, and oversized audio to the correct service errors | Client integration/failure | GenAI client test classes |
| Fetch localized exercises and forward bearer tokens correctly | Client integration | `LearningServiceClientTests` |
| Return a new user's empty aggregate through a real HTTP/database path | API integration | `ProgressFeedbackServiceApiIntegrationTests` |
| Return scoped progress through public HTTP and real PostgreSQL | Service E2E | `ProgressFeedbackServiceE2ETests` |
| Reject unauthenticated API traffic when auth is enabled | Security integration | `ProgressFeedbackServiceApplicationTests` |
| Start the Spring context and expose Prometheus metrics publicly | Application integration | `ProgressFeedbackServiceApplicationTests` |

`GenAiWritingClientManualTest` is intentionally excluded from automated results
because it calls a live external provider. The deterministic
`GenAiWritingClientTests` suite is the CI replacement for that path.

## GenAI coverage

GenAI route tests use FastAPI's `TestClient` against the assembled application,
so requests cross ASGI routing, middleware, request validation, error handlers,
response schemas, and router orchestration. External LLM, STT, and TTS providers
are replaced with deterministic fakes. The RAG engine tests use real BM25
indexing and local corpus JSON rather than mocking retrieval.

| Area | Covered behaviour | Test location |
| --- | --- | --- |
| Authentication and health | Public health/liveness/metrics, missing or invalid bearer tokens, valid tokens | `genai/tests/test_auth_middleware.py`, `test_llm_config.py` |
| Exercise generation | Valid structured responses, lesson ID propagation, required-field validation | `test_exercises.py` |
| Writing evaluation | Score/feedback mapping, score bounds, database text limits, target-language corrections, validation | `test_writing.py` |
| Speaking evaluation | STT/LLM/TTS behaviour, schema aliases and repairs, rubric scoring, missing/oversized audio, provider failures | `test_speaking.py` |
| Listening generation | Script/questions/options/audio, exactly one correct option, invalid schemas and levels, TTS/LLM failures | `test_listening.py` |
| Conversation practice | History, corrections, final turns, STT/LLM failures, missing/oversized inputs | `test_practice.py` |
| RAG HTTP retrieval and generation | Topics, corpus build/query routes, source grounding, lesson/type bounds, normalization, timeout handling | `test_rag.py` |
| RAG engine integration | Real BM25 ranking, source metadata, empty corpora, topic traversal protection, missing topics, topic discovery | `test_rag_engine.py` |
| GenAI service E2E | Public FastAPI request through schemas, router orchestration, real BM25 retrieval, controlled LLM boundary, and response mapping | `test_genai_e2e.py` |
| Interview RAG quality | Focused retrieval, attire rejection, corrective regeneration, second-review `502`, non-interview isolation | `test_rag.py`, `test_prompts.py` |
| Prompt contracts | Router variables, rubrics, quality rules, target language, and guardrails | `test_prompts.py` |

## Frontend coverage

| Area | Covered behaviour | Test location |
| --- | --- | --- |
| API client | Bearer headers, refreshed tokens, query encoding, JSON bodies, backend errors, network failures | `frontend/src/api/client.test.js` |
| Data mapping | Plans, lesson blocks, answers, feedback, progress summaries, and hidden listening plan | `frontend/src/api/mappers.test.js` |
| Authentication UI | Sign-in/registration actions, pending presentation, development bypass | `frontend/src/pages/AuthPage.test.jsx` |
| Registration onboarding | Begin, cancel, complete, and persisted onboarding state | `frontend/src/utils/onboarding.test.js` |
| Learner journey | Bypass auth, dashboard, current plan, lesson, and exercise in Chromium | `frontend/e2e/learning-flow.spec.js` |

## CI execution

| Workflow | Commands | Merge protection provided |
| --- | --- | --- |
| Automatic `.github/workflows/backend-ci.yml` | `./gradlew build -x test` in all three services | Lints OpenAPI and compiles/packages services without rerunning tests on every PR update |
| Manual `.github/workflows/backend-ci.yml` | `./gradlew test` in all three services | Runs backend unit, integration, and PostgreSQL Testcontainers E2E suites and uploads `backend-test-reports` |
| Automatic `.github/workflows/genai-ci.yml` | OpenAPI export/lint and Docker build | Validates contracts and packages GenAI without rerunning tests on every PR update |
| Manual `.github/workflows/genai-ci.yml` | `uv run pytest tests/ -v --junitxml=test-results/junit.xml` | Runs all deterministic GenAI tests and uploads `genai-test-report` |
| `.github/workflows/frontend-ci.yml` | `npm run test`, `npm run test:e2e`, `npm run build` | Runs unit/integration tests and the Chromium E2E flow before build and image creation |
| Backend and GenAI contract jobs | Redocly lint plus OpenAPI export diff | Reject invalid or stale API contracts |

Use **Run workflow** in GitHub Actions to produce fresh backend and GenAI test
reports before merging. The committed dated report records the current verified
baseline.

## Local commands

Run the same suites used by CI:

```bash
cd backend/user-service && ./gradlew test
cd backend/learning-service && ./gradlew test
cd backend/progress-feedback-service && ./gradlew test
cd genai && uv run pytest tests/ -v
cd frontend && npm run test
cd frontend && npm run test:e2e
```

On Windows, use `gradlew.bat test` for backend services. Docker must be running
for the PostgreSQL E2E cases. They are skipped, rather than failed, when Docker
is unavailable locally. The manual CI job first runs `docker info`, so a missing
CI Docker daemon fails the job instead of silently accepting skipped E2E tests.

## Verified baseline

The following baseline was verified locally on 2026-07-17 after adding the
per-service API integration and E2E tests:

| Suite | Result |
| --- | ---: |
| User service | 11 automated tests passed |
| Learning service | 51 automated tests passed |
| Progress-feedback service | 40 automated tests passed; one live-provider manual test skipped |
| Backend PostgreSQL service E2E | 3 classes compiled; execution skipped locally because Docker was unavailable |
| GenAI | 89 tests passed, including 1 service E2E |
| Frontend unit/integration | 20 tests passed |
| Frontend Playwright E2E | 1 test passed |

## Known boundary

Per-service unit, integration, and service E2E behaviour is automated. A full
Docker Compose test that starts PostgreSQL, MongoDB, Keycloak, all services, and
the frontend together is not currently part of pull-request CI because it would
require external model/provider credentials and substantially increase runtime.
The deterministic per-service tests and mocked cross-service clients remain
available through manual CI; deployment smoke checks remain a separate
operational concern.
