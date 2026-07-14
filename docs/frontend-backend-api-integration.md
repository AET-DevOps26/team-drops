# Frontend and Backend API Integration

This document describes the final integration boundary between the Team Drops
browser application and backend services. The authoritative schemas, methods,
and response codes remain in [`api/openapi.yaml`](../api/openapi.yaml).

## Request routing

The frontend uses relative paths so the same client code works locally and in
Kubernetes:

| Browser prefix | Local Vite target | Kubernetes service |
| --- | --- | --- |
| `/user-service` | `http://127.0.0.1:8081` | `user-service` |
| `/learning-service` | `http://127.0.0.1:8082` | `learning-service` |
| `/progress-service` | `http://127.0.0.1:8083` | `progress-feedback-service` |

Vite removes the prefix when proxying during development. Nginx Ingress applies
the equivalent routing in Rancher. Browser code must not use Docker-internal
service names directly.

## Authentication flow

Keycloak owns login, registration, logout, passwords, and access tokens.

```text
Browser
-> Keycloak Authorization Code Flow with PKCE
-> frontend receives an access token
-> frontend calls GET /user-service/api/v1/users/me
-> user-service resolves or creates the local application user
-> subsequent API calls include the bearer token
```

The frontend refreshes expiring tokens before protected requests. Backend
services validate JWT issuer and signing keys when `AUTH_ENABLED=true`.
Service-to-service requests forward the current bearer token when the downstream
operation acts on behalf of the user.

## Service ownership

| Service | Owns | Does not own |
| --- | --- | --- |
| User Service | Local user identity mapping and profile data | Passwords or Keycloak sessions |
| Learning Service | Plans, lessons, exercises, and generated learning content | User authentication or progress |
| Progress and Feedback Service | Answers, scores, feedback, and progress aggregation | Learning-plan definitions |
| GenAI Service | LLM, RAG, speech-to-text, and text-to-speech operations | Browser authentication flow or primary domain data |

PostgreSQL stores domain data for the three Spring services. MongoDB and the
document corpus support GenAI retrieval. Keycloak remains the identity source.

## Frontend flow

The current application integration follows this sequence:

1. Initialize Keycloak and obtain an access token when authentication is
   enabled.
2. Resolve the current application user through `/api/v1/users/me`.
3. Load the user profile, learning plans, and progress.
4. Load lesson content when the user opens a lesson.
5. Submit written or spoken answers to Progress and Feedback Service.
6. Refresh answer history, feedback, and progress from backend state.
7. Request listening content through Progress and Feedback Service, which owns
   the browser-facing orchestration boundary.

The frontend treats backend responses as the source of truth. It does not
substitute demo authentication, locally persisted profiles, or fake progress
when an API is unavailable.

## Browser API surface

The current request implementation uses:

```text
GET  /api/v1/users/me
GET  /api/v1/users/{user_id}/profile
PUT  /api/v1/users/{user_id}/profile

GET  /api/v1/learning-plans/user/{user_id}
POST /api/v1/learning-plans/default
GET  /api/v1/lessons/{lesson_id}

POST /api/v1/answers
POST /api/v1/answers/speaking
GET  /api/v1/answers/user/{user_id}
GET  /api/v1/answers/{answer_id}/feedback
GET  /api/v1/progress/user/{user_id}
POST /api/v1/listening/generate
```

Some list and progress operations accept language, plan, or target-language
query parameters. Consult OpenAPI rather than duplicating their schemas here.

## GenAI boundary

The browser does not call the GenAI service, Ollama, or an OpenAI-compatible
provider directly. Learning and Progress services orchestrate GenAI operations
and return domain API responses to the frontend. This keeps provider URLs and
credentials out of the browser and centralizes validation and persistence.

Direct `/api/v1/genai/*` routes remain in the central contract for internal
service integration and operational testing. They are routed separately from
the three browser service prefixes.

## Error handling

The frontend API client:

- sends `Accept: application/json` and JSON content type where appropriate;
- includes the current bearer token when one is available;
- distinguishes an unreachable backend from an HTTP error response;
- uses the API response message when supplied;
- does not silently replace failures with demonstration data.

Exact validation-error schemas and status codes are defined by OpenAPI and the
service implementation.

## Contract-change workflow

1. Design the change in `api/openapi.yaml`.
2. Update the affected service implementation and exported service contract.
3. Update `frontend/src/api/client.js`, `mappers.js`, and the consuming UI when
   the browser contract changes.
4. Run `bash api/scripts/export_openapi.sh` from the repository root.
5. Run backend, frontend, and GenAI checks relevant to the change.
6. Commit synchronized contract and implementation changes together.

Do not manually edit generated backend interfaces or models. The frontend does
not currently commit a generated TypeScript client; its request and mapping
layers must remain aligned with OpenAPI during review.
