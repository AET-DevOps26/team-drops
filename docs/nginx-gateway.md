# NGINX Gateway

The project uses NGINX as the gateway for browser traffic. The frontend does
not call backend services by internal host or port. Instead, frontend requests
use relative paths on the same origin, and NGINX routes them to the correct
internal service.

Implementation files:

- Local Docker NGINX routing: [`frontend/nginx.conf`](../frontend/nginx.conf)
- Kubernetes NGINX Ingress routing: [`helm/team-drops/templates/ingress.yaml`](../helm/team-drops/templates/ingress.yaml)
- Helm service names and path prefixes: [`helm/team-drops/values.yaml`](../helm/team-drops/values.yaml)
- Frontend API base paths: [`frontend/src/api/client.js`](../frontend/src/api/client.js)
- Optional gateway auth service: [`helm/team-drops/templates/oauth2-proxy.yaml`](../helm/team-drops/templates/oauth2-proxy.yaml)

## Local Docker

In local Docker, the frontend container serves the React app through NGINX on
port `3000`.

Browser entrypoint:

```text
http://localhost:3000
```

Routes:

```text
/                         -> React frontend
/user-service/...          -> user-service:8081
/learning-service/...      -> learning-service:8082
/progress-service/...      -> progress-feedback-service:8083
```

Example:

```text
Browser request:
GET http://localhost:3000/user-service/api/v1/users/me

NGINX forwards to:
GET http://user-service:8081/api/v1/users/me
```

The frontend does not call GenAI directly in local Docker. GenAI is called by
backend services, such as `progress-feedback-service`, when needed.

Implementation: [`frontend/nginx.conf`](../frontend/nginx.conf)

## Kubernetes

In Kubernetes, NGINX Ingress is the public entrypoint for the application.

Routes:

```text
/                         -> frontend
/user-service/...          -> user-service
/learning-service/...      -> learning-service
/progress-service/...      -> progress-feedback-service
/api/v1/genai/...          -> genai-service
/oauth2/...                -> oauth2-proxy, if enabled
```

Example:

```text
Browser request:
GET https://your-domain.com/learning-service/api/v1/lessons/1

NGINX Ingress forwards to:
GET http://learning-service/api/v1/lessons/1
```

Implementation: [`helm/team-drops/templates/ingress.yaml`](../helm/team-drops/templates/ingress.yaml)

## Production Gateway Authentication

For production, NGINX Ingress can centralize token validation before requests
reach backend services. This is done with NGINX external auth and
`oauth2-proxy`.

Production request flow:

```text
Browser
-> NGINX Ingress
-> /oauth2/auth on oauth2-proxy
-> Keycloak session or Bearer token validation
-> backend service only if authentication succeeds
```

Protected gateway routes:

```text
/user-service/...
/learning-service/...
/progress-service/...
/api/v1/genai/...
```

Public gateway routes:

```text
/
/oauth2/...
/user-service/actuator/health
/user-service/v3/api-docs/...
/user-service/swagger-ui/...
/user-service/swagger-ui.html
```

Recommended production Helm values:

```yaml
auth:
  enabled: true

oauth2Proxy:
  enabled: false

keycloak:
  enabled: true
```

Use [`helm/team-drops/values-rancher.yaml`](../helm/team-drops/values-rancher.yaml)
for the deployed host, TLS, Keycloak redirect, and service resource overrides.
Provide secrets through GitHub repository secrets and namespaced Kubernetes
Secrets; do not commit them to a values file.

If Keycloak is managed outside this chart, keep `keycloak.enabled=false` and
set the external issuer/client settings in the auth and oauth2-proxy values.

The current Rancher profile uses service-level JWT validation. Enable
`oauth2Proxy.enabled` only when the cluster is configured for the optional
gateway-level flow described below.

## Authentication

The frontend sends the Keycloak access token with backend requests:

```http
Authorization: Bearer <access-token>
```

NGINX forwards this header to the backend service. Each backend service still
validates the JWT itself when `AUTH_ENABLED=true`.

This means NGINX is the routing gateway, but backend services do not blindly
trust the gateway.

Frontend API path implementation: [`frontend/src/api/client.js`](../frontend/src/api/client.js)

## Optional Gateway-Level Auth

The Helm chart can enable `oauth2-proxy` for NGINX Ingress external auth.

When `auth.enabled=true` and `oauth2Proxy.enabled=true`, NGINX checks protected
routes before forwarding them:

```text
/user-service/...
/learning-service/...
/progress-service/...
/api/v1/genai/...
```

Public routes remain reachable, such as:

```text
/
/oauth2/...
/user-service/actuator/health
/user-service/v3/api-docs/...
/user-service/swagger-ui/...
```

Gateway-level auth is an additional checkpoint. Service-level JWT validation
remains required when authentication is enabled.

Implementation: [`helm/team-drops/templates/oauth2-proxy.yaml`](../helm/team-drops/templates/oauth2-proxy.yaml)

## Why This Design

This gateway setup provides:

- one public entrypoint
- no direct browser access to internal services
- fewer CORS issues
- centralized path routing
- optional gateway-level authentication
- service-level JWT validation for defense in depth

## Best Practice Compliance

This project follows the service discovery and API gateway rule for
user-facing traffic: all browser requests enter through NGINX and are routed by
path. The frontend uses relative API paths and does not call Docker or
Kubernetes internal service hostnames directly.

Gateway-routed API paths:

```text
/user-service/...          -> user-service
/learning-service/...      -> learning-service
/progress-service/...      -> progress-feedback-service
/api/v1/genai/...          -> genai-service
```

Implementation files:

- Gateway routes: [`helm/team-drops/templates/ingress.yaml`](../helm/team-drops/templates/ingress.yaml)
- Frontend API paths: [`frontend/src/api/client.js`](../frontend/src/api/client.js)

Protected routes can be checked at the gateway by NGINX Ingress external auth
through `oauth2-proxy`. Backend services still validate JWTs themselves, so the
gateway is the first checkpoint, not the only trust boundary.

Security implementation files:

- Gateway auth service: [`helm/team-drops/templates/oauth2-proxy.yaml`](../helm/team-drops/templates/oauth2-proxy.yaml)
- Keycloak and JWT flow: [`docs/keycloak-authentication.md`](keycloak-authentication.md)

The frontend never calls GenAI, Ollama, OpenAI, or internal service hostnames
directly. For writing evaluation, the browser calls the progress API through
NGINX, and the backend owns the AI integration.

Writing evaluation flow:

```text
Browser
-> NGINX Ingress /progress-service/api/v1/answers
-> progress-feedback-service /api/v1/answers
-> learning-service /api/v1/lessons/{lessonId}
-> genai-service /api/v1/genai/writing/evaluate
-> progress-feedback-service stores score and feedback
-> Browser receives the answer/feedback response
```

The Java-to-Python writing evaluation call is not an unmanaged frontend or
ad-hoc cross-language dependency. It is encapsulated behind the
progress-feedback service integration client, keeps the OpenAPI route contract,
forwards the user Bearer token when authentication is enabled, and has focused
tests.

Integration implementation files:

- Writing orchestration: [`backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/service/UserAnswerService.java`](../backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/service/UserAnswerService.java)
- Learning context lookup: [`backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/integration/LearningServiceClient.java`](../backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/integration/LearningServiceClient.java)
- GenAI writing integration: [`backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/integration/GenAiWritingClient.java`](../backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/integration/GenAiWritingClient.java)
- Bearer token forwarding helper: [`backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/security/CurrentBearerTokenResolver.java`](../backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/security/CurrentBearerTokenResolver.java)
