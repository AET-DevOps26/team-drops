# Local Auth + AI Test Report

Date: 2026-07-17
Branch/worktree: `hardening/rag` in `/private/tmp/team-drops-rag-hardening`

## Environment

- Frontend: `http://localhost:3000`
- Auth: enabled with Keycloak realm `team-drops`
- Local QA account: `qa-local-rag` / `qa+local-rag@example.com`
- Docker services: frontend, Keycloak, user-service, learning-service, progress-feedback-service, GenAI, PostgreSQL, MongoDB
- Final runtime used for successful RAG E2E: OpenAI-compatible external API via `LLM_PROVIDER=openai`, `LLM_API_KEY`, `LLM_MODEL=openai/gpt-oss-120b`, and `LLM_BASE_URL=https://logos.aet.cit.tum.de/v1`
- Earlier comparison runtime: host/external Ollama via `OLLAMA_BASE_URL=http://host.docker.internal:11434`, model `qwen3:1.7b`
- Docker Ollama was also tested with `llama3.2:1b` on host port `11435`

## Summary

The authenticated Docker stack starts and the core auth, RAG, reading, listening, writing, and speaking routes work with the API-key-backed external model. The original speaking target-language issue is fixed: generated speaking submissions can request German/English exercise context and pass `target_language` through to GenAI.

Two important quality findings remain:

- Docker-local `llama3.2:1b` is too slow for the structured RAG learning-plan flow, timing out at 90s and 180s even with reduced context.
- The earlier smaller host Ollama model could generate and persist a plan, but RAG content quality was mixed. The API-key model produced German software-engineering interview plans with all four requested exercise types in the final runs.

## Fresh Final Rerun

After the final review fixes, the stack was rebuilt and recreated from this branch with auth enabled and the external OpenAI-compatible API-key runtime. The temp worktree does not have its own `.env`, so the run used `/Users/juliankraus/Coding/Coding-Uni/DevOps/team-drops/.env` plus `AUTH_ENABLED=true`.

- Rebuild/startup: pass. All app services, Keycloak, PostgreSQL, and MongoDB were running after `docker compose up --build -d`.
- Migration compatibility: the rerun exposed that existing learning-service databases can see the new `V0_1` bootstrap migration as a lower-version ignored migration. Added `spring.flyway.ignore-migration-patterns=*:ignored`; clean Docker startup then validated 4 migrations and started successfully without editing V1/V2 checksums.
- Auth: pass. Missing tokens return `401`; QA token for `qa-local-rag` returns user `2` (`QA RAG`) from `/api/v1/users/me`.
- GenAI auth: pass. Missing token on `/api/v1/genai/rag/topics` returns `401`; QA token returns topics including `job interview`.
- RAG generation: pass. Created plan `1`, `Deutsch B1 – Vorbereitung auf Software‑Engineering‑Interviews`, for user `2`.
- Generated lessons: pass. Lesson `1` contained reading, writing, speaking, and listening; lesson `2` contained writing, speaking, listening, and an additional writing task.
- Reading submission: pass. Exercise `1` submitted with answer `B`, returned score `100`, persisted answer `1`.
- Listening generation/submission: pass. First 180s client attempt timed out while GenAI continued processing; retry with a longer timeout completed in about 109s, returned 4 questions plus audio, submitted successfully with score `100`, persisted answer `2`.
- Writing submission: pass. Exercise `2` submitted in German, returned score `75`, persisted answer `3` with useful feedback about missing backend API/user-authentication details.
- Speaking submission: pass for the fixed behavior. Silent German and English submissions both reached GenAI with the requested target language and returned sanitized `400 No speech was detected in the uploaded audio.` No speaking answer was persisted.
- Browser smoke: pass. Keycloak login via the UI succeeded; dashboard rendered `Welcome back, QA RAG`, the generated current plan, the two lessons, and lesson 1's reading/writing/speaking/listening exercise cards with the submitted scores.
- Logs: pass. Learning-service validated migrations and started cleanly; progress-feedback handled reading/listening/writing/speaking calls; GenAI showed expected 401/200 auth behavior and no crash. Expected local warnings remained for ONNX CPU vendor, unauthenticated HF Hub access, and TTS returning no audio for silent speaking evaluation.

## Changes Validated

- Keycloak local HTTP auth works after setting local realm SSL to `none`.
- Compose works with multiple checkouts by removing hardcoded container names.
- Compose defaults to the API-key-backed external provider and keeps the internal Ollama service behind the opt-in `local-ollama` profile.
- Learning service validates the current authenticated user in Docker via `USER_SERVICE_URL=http://user-service:8081`.
- Learning-service schema bootstrap lets clean databases run Flyway before Hibernate without editing existing migration checksums.
- RAG calls send a smaller `top_k` context window and propagate downstream GenAI error messages.
- RAG learning-plan generation now preserves reading multiple-choice tasks and adds fallback exercises for any requested type the model omits.
- Frontend and Spring error parsing now surface `message`, `detail`, or `error`.
- RAG UI no longer disables reading/listening exercise requests.
- Speaking submissions request the selected target-language exercise context.
- Blank speaking transcriptions are rejected with `400 No speech was detected in the uploaded audio.` and are not persisted.

## Manual / E2E Execution

The following checks were exercised manually against the local Docker stack with
authentication enabled and the API-key-backed GenAI runtime.

| Area | Result | Notes |
| --- | --- | --- |
| Docker startup | Pass | All services running after rebuild; health endpoints for learning/progress and Keycloak responded. |
| Auth registration/login | Pass | Registered and logged in as `QA LocalRag`; dashboard showed authenticated user. |
| RAG UI exercise types | Pass | Reading, listening, writing, and speaking are selectable. |
| RAG topics | Pass | `job interview` and `Reisen in der Schweiz` available. |
| RAG generation with Docker `llama3.2:1b` | Fail/blocked by local model speed | Timed out at 90s and 180s. Error now surfaces more clearly. |
| RAG generation with API-key model | Pass | Fresh final rerun created plan `1`: Deutsch B1 – Vorbereitung auf Software‑Engineering‑Interviews, with all requested exercise types represented. |
| RAG generation with host `qwen3:1.7b` | Pass with quality caveat | Created plan `5`: German Software Engineering Interview Preparation. |
| Generated plan persistence | Pass | Fresh final rerun plan `1`, lessons `1`-`2`, and exercises `1`-`9` persisted in PostgreSQL. |
| Generated exercise types | Pass | Final API-key result included reading, listening, writing, and speaking; reading choices were not duplicated. |
| Reading submission | Pass | Fresh final rerun exercise `1`, submitted with answer `B`; returned score `100` and saved answer `1`. |
| Listening generation/submission | Pass | Fresh final rerun exercise `4`, generated 4 questions and audio on retry; submitted selections returned score `100` and saved answer `2`. |
| Writing submission | Pass | Fresh final rerun exercise `2`, submitted with `target_language=German`; returned score `75` and saved answer `3`. |
| Speaking submission target language | Pass | Fresh final rerun exercise `3` hit GenAI with German and English target-language values; no target-language parameter error. |
| Speaking no-speech behavior | Fixed | Silent audio returns `400 No speech was detected in the uploaded audio.` and does not create a new answer. |
| Logs | Pass with expected warnings | No service crash after final rebuild; GenAI logs HF unauthenticated warning and ONNX CPU vendor warning. One local TTS warning was logged during speaking evaluation, but the route still returned the expected no-speech error. |

## Fresh Final Rerun Records

- User: `2` (`QA RAG`)
- Plan: `1` (`Deutsch B1 – Vorbereitung auf Software‑Engineering‑Interviews`)
- Lessons:
  - `1`: `Selbstvorstellung & Verhaltensfragen`, 4 exercises
  - `2`: `Projekt‑ und Fachfragen (Backend‑API, Debugging, System‑Design)`, 5 exercises
- Lesson `1` exercises:
  - `1`: reading/multiple choice, German B1
  - `2`: writing/translation, German B1
  - `3`: speaking prompt, German B1
  - `4`: listening choice, German B1
- Persisted answers:
  - `1`: reading answer for exercise `1`, score `100`
  - `2`: listening answer for exercise `4`, score `100`
  - `3`: writing answer for exercise `2`, score `75`
  - No persisted answer for the no-speech speaking attempts on exercise `3`

## Earlier Generated RAG Records

- User: `1` (`QA LocalRag`)
- Final generation plan: `9` (`Software-Engineering Interview Preparation (German, B1)`)
- Final generation lesson: `29`
- Final generation exercises:
  - `74`: reading/multiple choice, German B1
  - `75`: listening choice, German B1
  - `76`: writing/free text, German B1
  - `77`: speaking prompt, German B1
- Submission-flow plan: `7` (`Software-Engineering Interview Vorbereitung - Deutsch B1`)
- Submission-flow lesson: `25`
- Exercises:
  - `61`: reading/multiple choice, German B1
  - `62`: listening choice, German B1
  - `63`: writing/free text, German B1
  - `64`: speaking prompt, German B1
- Persisted answers:
  - `4`: reading answer for exercise `61`, score `100`
  - `5`: writing answer for exercise `63`, score `100`
  - `6`: listening answer for exercise `62`, score `100`
  - No persisted answer for the no-speech speaking attempts on exercise `64`

## Automated Verification

All focused automated checks passed. Docker Compose startup, Keycloak auth,
browser navigation, and live API-key GenAI flows are covered by the manual E2E
table above rather than by these unit/integration commands.

```text
backend/learning-service:
./gradlew test --tests de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClientTests --tests de.tum.aet.devops26.learning_service.integration.UserServiceClientTests --tests de.tum.aet.devops26.learning_service.service.LearningPlanServiceTests --tests de.tum.aet.devops26.learning_service.service.LessonServiceTests

backend/progress-feedback-service:
./gradlew test --tests de.tum.aet.devops26.progress_feedback_service.service.UserAnswerServiceTests

genai:
uv run python -m pytest tests/test_rag.py tests/test_llm_config.py tests/test_auth_middleware.py

frontend:
npm run lint
npm test -- src/api/mappers.test.js src/api/client.test.js
```

## Open Follow-Ups

- Improve RAG grounding/prompting for `job interview` so generated German software-engineering interview exercises stay consistently technical and do not drift into generic interview advice.
- Consider making reading multiple-choice options structured instead of embedded in the question text.
- Listening generation can exceed 180s with TTS/audio creation; keep the longer client/server timeout path or consider an async generation flow if this becomes a UX problem.
- Keep using `localhost` for the Keycloak token endpoint in local curl tests; tokens issued from `127.0.0.1` have an issuer mismatch with the Spring services.
