# Local Auth + AI QA Test Plan

## Scope

Validate the full local Docker stack with authentication enabled, covering:

- Keycloak registration/login/logout and token-protected service calls.
- Profile and target-language changes for English and German.
- Default learning plans, lesson navigation, and progress loading.
- RAG learning-plan generation for reading, listening, writing, and speaking.
- Generated reading, listening, writing, and speaking exercise behavior.
- GenAI health, RAG topics/query/learning-plan APIs, LLM timeout behavior, and service-to-service auth forwarding.
- Answer submission, feedback persistence, progress updates, and saved answer review.

## Environment

- Branch/worktree: `hardening/rag` in `/private/tmp/team-drops-rag-hardening`
- Docker Compose with `AUTH_ENABLED=true`
- Local Keycloak realm: `team-drops`
- Local LLM: OpenAI-compatible external provider with `LLM_PROVIDER=openai`, `LLM_API_KEY`, and `LLM_MODEL`
- Browser URL: `http://localhost:3000`

## Preconditions

1. Docker daemon is running.
2. `LLM_API_KEY` is available in the local environment or `.env` file, and `LLM_MODEL` names a model supported by that API.
3. Ports `3000`, `5433`, `8081`, `8082`, `8083`, `8084`, `8090`, `11434`, and `27017` are free or the conflicting services are intentionally stopped.
4. No real credentials are committed. Test account uses disposable data.

## Test Account

- Email: `qa+local-rag@example.com`
- Username: `qa-local-rag`
- Password: `QaLocalRag!2026`
- First name: `QA`
- Last name: `RAG`

If this account already exists, log in with it or create a fresh timestamped variant.

## Execution Matrix

### A. Stack Startup

1. Start Docker Compose with authentication enabled.
2. Verify containers are healthy/running: PostgreSQL, MongoDB, Keycloak, user-service, learning-service, progress-feedback-service, GenAI, and frontend.
3. Verify health endpoints:
   - `GET http://localhost:8081/actuator/health`
   - `GET http://localhost:8082/actuator/health`
   - `GET http://localhost:8083/actuator/health`
   - `GET http://localhost:8084/health`
4. Confirm GenAI readiness reports only sanitized status, for example `{"status":"ok"}`.
   If required LLM configuration is missing, it should return `503` with `{"status":"degraded"}`.
   Confirm provider/model/timeout through Compose configuration or restricted logs when needed.

### B. Authentication

1. Open the frontend.
2. Register the test account through Keycloak.
3. Confirm redirect back to the app succeeds.
4. Confirm the app calls `GET /user-service/api/v1/users/me` and creates/loads the local user.
5. Log out and log back in.
6. Confirm unauthenticated API calls to protected endpoints return `401`.

### C. Profile + Language

1. Open settings/profile.
2. Set learning goal to: `Prepare for software engineering interviews`.
3. Set current level to `B1` if editable; otherwise note the current level.
4. Switch target language to German and confirm plans reload.
5. Switch target language to English and confirm plans reload.
6. Switch back to German for the RAG workflow.

### D. Default Plan Exercises

1. Open a default/training plan.
2. Open a lesson and inspect reading/writing/listening/speaking cards where present.
3. Submit a writing answer and verify:
   - feedback returns,
   - score/progress changes,
   - saved answer review persists after navigating away/back.
4. Open a listening exercise and verify content/audio/questions load.
5. Submit listening selections and verify scoring/progress.
6. Open a speaking exercise and verify recording/upload controls render.
7. Submit a small generated/selected audio sample if STT is available; otherwise document STT dependency failure and verify graceful error display.

### E. RAG Plan Generation

1. Open RAG learning.
2. Confirm available topics include `job interview` and `Reisen in der Schweiz`.
3. Select `job interview`.
4. Confirm reading, listening, writing, and speaking exercise types are selectable.
5. Generate a plan with:
   - goal: `Prepare for German software engineering interviews with project and system design explanations`
   - duration: 2 weeks
   - study time: 3 hours/week
   - min lessons: 1
   - max lessons: 2
   - exercise types: reading + listening + writing + speaking
6. Confirm created plan appears in Training.
7. Inspect generated lessons:
   - content blocks are readable,
   - exercises are self-contained,
   - exercises are software-engineering/interview related,
   - no unsupported exercise type/subtype appears.
8. Submit a generated reading exercise.
9. Generate and submit a generated listening exercise.
10. Submit a generated writing exercise.
11. Submit or smoke-test a generated speaking exercise.

### F. Direct API Checks

1. Obtain a user access token through the browser/session or Keycloak.
2. Verify `GET /api/v1/genai/rag/topics` with token.
3. Verify `POST /api/v1/genai/rag/query` for `job interview`.
4. Verify `POST /api/v1/genai/rag/learning-plan` accepts reading/listening/writing/speaking.
5. Verify invalid/malformed generated exercise shapes are normalized by schema tests already covered in automated test suite.
6. Verify protected GenAI/Learning/Progress routes reject missing bearer tokens.

### G. Regression + Observability

1. Check Docker logs for unhandled exceptions in every service.
2. Check frontend console for JavaScript errors.
3. Restart frontend and services, then reload the app.
4. Confirm saved progress and generated plan persist.

## Pass Criteria

- Authenticated user can complete core app navigation with no unexpected 4xx/5xx errors.
- RAG generated plans are persisted and usable.
- RAG UI allows all four supported exercise requests.
- Speaking submissions use the selected target language context.
- GenAI APIs return useful responses or clear, surfaced errors when the local model/STT dependency cannot complete.
- No service repeatedly crashes or logs uncaught exceptions during the run.

## Known Local Risks

- API-key-backed model calls depend on the external provider's latency and quota. The Compose-internal Ollama service is opt-in with the `local-ollama` profile.
- Local STT/TTS can trigger large model downloads; if they fail due local resources or network, record as environmental unless the app surfaces the error badly.
- Small local models may produce low-quality learning content; assess whether schema, grounding, and UI remain robust even if prose quality is modest.
