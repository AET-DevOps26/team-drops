# Testing Strategy and Coverage

This document is the test inventory for InterviewMate. It explains which system
behaviours are protected, where those tests live, and which CI job prevents a
regression from being merged.

## Policy

Testing validates observable behaviour rather than implementation details.
Critical server-side and GenAI logic requires automated unit tests. Each backend
microservice also requires an integration test that crosses its real HTTP,
controller, service, serialization, and persistence layers. Client-side tests
cover core interactions, and one Playwright scenario protects the primary
learner journey.

All automated suites run on pull requests to `main`. A failing required CI check
must block merging.

| Aspect | Repository requirement | Enforcement |
| --- | --- | --- |
| Backend unit tests | Cover critical service rules, edge cases, and failures | `Backend CI / build-backend-services` |
| Backend integration tests | Exercise every microservice through a real HTTP server and isolated H2 database | `Backend CI / build-backend-services` |
| Backend security/operations tests | Protect API authentication and public Prometheus endpoints | `Backend CI / build-backend-services` |
| GenAI unit and API integration tests | Cover routes, schemas, prompts, provider failures, validation, authentication, RAG quality, and the local retrieval engine | `GenAI CI / test` |
| Client tests | Cover API mapping, request failures, auth presentation, and the primary learner workflow | `Frontend CI / build-frontend` |
| Contract tests | Lint the combined OpenAPI contract and reject stale generated GenAI specifications | `Backend CI / openapi-lint` and `GenAI CI / sync-openapi` |
| Build verification | Compile/package applications and build deployable images only after tests pass | Backend, GenAI, and Frontend CI build jobs |

## Backend coverage

Backend tests use JUnit 5, AssertJ, Mockito, Spring Boot, and H2. Outbound client
tests use local stub HTTP servers. API integration tests use
`@SpringBootTest(webEnvironment = RANDOM_PORT)` and `TestRestTemplate`, so they
exercise the application as a network client would without depending on shared
infrastructure.

### User service

| Behaviour | Level | Automated coverage |
| --- | --- | --- |
| Create an OIDC user on first login; reuse by Keycloak subject; link an existing email | Unit | `UserServiceTests` |
| Create and consistently reuse the local development user | Unit + API integration | `UserServiceTests`, `UserServiceApiIntegrationTests` |
| Create default profiles and return existing profiles | Unit | `UserProfileServiceTests` |
| Persist and serialize `/api/v1/users/me` across two real HTTP calls | API integration | `UserServiceApiIntegrationTests` |
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
| `.github/workflows/backend-ci.yml` | `./gradlew build` in all three services | Runs unit and integration tests, compiles/packages each service, uploads HTML test reports even on failure, then gates Docker builds |
| `.github/workflows/genai-ci.yml` | `uv run pytest tests/ -v --junitxml=test-results/junit.xml` | Runs all deterministic GenAI tests, uploads the JUnit report, then gates OpenAPI synchronization and image build |
| `.github/workflows/frontend-ci.yml` | `npm run test`, `npm run test:e2e`, `npm run build` | Runs unit/integration tests and the Chromium E2E flow before build and image creation |
| Backend and GenAI contract jobs | Redocly lint plus OpenAPI export diff | Reject invalid or stale API contracts |

Backend CI deliberately uses `./gradlew build`, not `./gradlew build -x test`.
Test reports are uploaded as the `backend-test-reports` artifact to make failures
reviewable from the GitHub Actions run.

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

On Windows, use `gradlew.bat test` for backend services.

## Verified baseline

The following baseline was verified locally on 2026-07-17 after adding the
per-service API integration tests:

| Suite | Result |
| --- | ---: |
| User service | 11 automated tests passed |
| Learning service | 51 automated tests passed |
| Progress-feedback service | 40 automated tests passed; one live-provider manual test skipped |
| GenAI | 88 tests passed |
| Frontend unit/integration | 20 tests passed |
| Frontend Playwright E2E | 1 test passed |

## Known boundary

Per-service integration and client-contract behaviour are automated. A full
Docker Compose test that starts PostgreSQL, MongoDB, Keycloak, all services, and
the frontend together is not currently part of pull-request CI because it would
require external model/provider credentials and substantially increase runtime.
The deterministic per-service tests and mocked cross-service clients remain the
merge gate; deployment smoke checks remain a separate operational concern.
