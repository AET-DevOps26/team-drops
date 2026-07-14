# Current API Inventory

This inventory describes the final repository state. The authoritative
machine-readable contract is [`api/openapi.yaml`](../api/openapi.yaml); the files under
[`api/services`](../api/services/) contain the service-specific contracts.

The browser calls the three domain services through relative gateway prefixes.
GenAI endpoints are used by backend orchestration and are not called directly
from browser code.

## Browser-facing APIs

| Service | Gateway prefix | API operations used by the frontend |
| --- | --- | --- |
| User | `/user-service` | Current user bootstrap, profile retrieval, and profile update |
| Learning | `/learning-service` | User learning plans, default-plan creation, and lesson details |
| Progress and feedback | `/progress-service` | Written and spoken answers, answer history, feedback, progress, and listening generation |

Current frontend operations:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/users/me` | Resolve or create the local user associated with the Keycloak identity |
| `GET` | `/api/v1/users/{user_id}/profile` | Load a user profile |
| `PUT` | `/api/v1/users/{user_id}/profile` | Update a user profile |
| `GET` | `/api/v1/learning-plans/user/{user_id}` | Load plans for a user and optional language |
| `POST` | `/api/v1/learning-plans/default` | Create a default learning plan |
| `GET` | `/api/v1/lessons/{lesson_id}` | Load lesson content and exercises |
| `POST` | `/api/v1/answers` | Submit a written exercise answer |
| `POST` | `/api/v1/answers/speaking` | Submit speaking audio for evaluation |
| `GET` | `/api/v1/answers/user/{user_id}` | Load a user's answer history |
| `GET` | `/api/v1/answers/{answer_id}/feedback` | Load feedback for an answer |
| `GET` | `/api/v1/progress/user/{user_id}` | Load progress, optionally scoped to a plan and language |
| `POST` | `/api/v1/listening/generate` | Generate a listening exercise through backend orchestration |

The frontend implementation is in
[`frontend/src/api/client.js`](../frontend/src/api/client.js). It refreshes the
Keycloak token before protected calls and sends requests through same-origin
gateway paths.

## Backend orchestration APIs

The central contract also contains operations that are not called by the
current browser client:

- AI learning-plan creation and AI exercise generation in Learning Service
- GenAI exercise generation and writing evaluation
- Speaking evaluation and practice generation
- Listening generation
- RAG topic, corpus, and query operations
- GenAI health checks

Progress and Learning services call GenAI internally where their workflows need
generation or evaluation. Browser code must not contain provider credentials or
call Ollama/OpenAI endpoints directly.

## Authentication

Keycloak owns login and registration. There is no frontend email/password login
request to User Service. With authentication enabled, protected API calls carry:

```http
Authorization: Bearer <access-token>
```

See [Keycloak authentication](keycloak-authentication.md) for the complete
authorization-code flow and service token validation.

## Maintaining the inventory

For an API change:

1. Update `api/openapi.yaml` first.
2. Update the affected service contract and implementation.
3. Update `frontend/src/api/client.js` and `mappers.js` when browser behavior
   changes.
4. Run `bash api/scripts/export_openapi.sh`.
5. Commit the synchronized central and service contracts.

Do not use this Markdown inventory as a replacement for the OpenAPI contract.
