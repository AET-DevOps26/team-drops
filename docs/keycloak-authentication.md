# Keycloak Authentication

The project uses Keycloak as the identity provider. Keycloak owns real user
accounts, passwords, login, registration, access tokens, and the public signing
keys used to verify those tokens.

The app database does not store Keycloak passwords. It stores the local
application user/profile data that the learning and progress services need.

Implementation files:

- Frontend OIDC wrapper: [`frontend/src/auth/keycloak.js`](../frontend/src/auth/keycloak.js)
- Frontend auth screen: [`frontend/src/pages/AuthPage.jsx`](../frontend/src/pages/AuthPage.jsx)
- Frontend auth state: [`frontend/src/App.jsx`](../frontend/src/App.jsx)
- Frontend API token forwarding: [`frontend/src/api/client.js`](../frontend/src/api/client.js)
- User bootstrap endpoint: [`backend/user-service/src/main/java/de/tum/aet/devops26/user_service/api/impl/UserServiceController.java`](../backend/user-service/src/main/java/de/tum/aet/devops26/user_service/api/impl/UserServiceController.java)
- User mapping logic: [`backend/user-service/src/main/java/de/tum/aet/devops26/user_service/service/UserService.java`](../backend/user-service/src/main/java/de/tum/aet/devops26/user_service/service/UserService.java)
- Local user model: [`backend/user-service/src/main/java/de/tum/aet/devops26/user_service/model/User.java`](../backend/user-service/src/main/java/de/tum/aet/devops26/user_service/model/User.java)
- User lookup repository: [`backend/user-service/src/main/java/de/tum/aet/devops26/user_service/repository/UserRepository.java`](../backend/user-service/src/main/java/de/tum/aet/devops26/user_service/repository/UserRepository.java)

## Login Flow

When authentication is enabled, the frontend uses Keycloak Authorization Code
Flow with PKCE.

```text
Browser
-> frontend
-> Keycloak login or registration page
-> frontend receives Keycloak access token
-> frontend calls backend APIs with Authorization: Bearer <token>
-> user-service /api/v1/users/me creates or links the local app user
-> backend services validate the JWT before handling protected requests
```

The frontend does not send an email/password login request to `user-service`.
Sign in and registration both redirect to Keycloak.

Key implementation points:

- `frontend/src/auth/keycloak.js` initializes `keycloak-js`, starts login,
  starts registration, logs out, and refreshes tokens before API calls.
- `frontend/src/api/client.js` adds `Authorization: Bearer <token>` to backend
  requests when a token is available.
- `backend/user-service/.../UserServiceController.java` exposes
  `GET /api/v1/users/me`.
- `backend/user-service/.../UserService.java` maps Keycloak claims to a local
  `User`.

## Where Users Live

Real accounts live in Keycloak. Use the Keycloak admin UI to see users,
passwords, credentials, and account settings.

Local app users live in the user-service database. They are created or linked
when the frontend calls:

```text
GET /user-service/api/v1/users/me
```

The local user row stores app-specific identity data such as:

- local numeric user id
- name
- email
- `keycloakSubject`

The `keycloakSubject` links the local user to the Keycloak `sub` claim.

`passwordHash` is legacy/local-dev compatible data and should not be used for
Keycloak login. Keycloak is the source of truth for passwords when auth is
enabled.

## JWT Validation

When `AUTH_ENABLED=true`, protected backend routes require a valid Bearer JWT.
The Spring services validate JWTs with Spring Security OAuth2 Resource Server:

- [`backend/user-service/src/main/java/de/tum/aet/devops26/user_service/config/SecurityConfig.java`](../backend/user-service/src/main/java/de/tum/aet/devops26/user_service/config/SecurityConfig.java)
- [`backend/learning-service/src/main/java/de/tum/aet/devops26/learning_service/config/SecurityConfig.java`](../backend/learning-service/src/main/java/de/tum/aet/devops26/learning_service/config/SecurityConfig.java)
- [`backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/config/SecurityConfig.java`](../backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/config/SecurityConfig.java)

GenAI validates JWTs in FastAPI middleware:

- [`genai/app/middleware/auth.py`](../genai/app/middleware/auth.py)
- [`genai/app/config.py`](../genai/app/config.py)

The token issuer and JWKS URL are intentionally separate. The issuer is the URL
inside the token. The JWKS URL is where the service fetches signing keys.

For local Docker, the browser-visible issuer is:

```text
http://localhost:8090/realms/team-drops
```

The container-visible JWKS URL is:

```text
http://keycloak:8080/realms/team-drops/protocol/openid-connect/certs
```

This split lets browsers use `localhost` while containers fetch signing keys
through the Docker network.

## Service-To-Service Token Forwarding

Some backend requests trigger internal service calls. Those calls must keep the
same user Bearer token when authentication is enabled.

Implementation files:

- [`backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/security/CurrentBearerTokenResolver.java`](../backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/security/CurrentBearerTokenResolver.java)
- [`backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/integration/LearningServiceClient.java`](../backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/integration/LearningServiceClient.java)
- [`backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/integration/GenAiWritingClient.java`](../backend/progress-feedback-service/src/main/java/de/tum/aet/devops26/progress_feedback_service/integration/GenAiWritingClient.java)

The progress-feedback service forwards the current `Authorization` header when
it calls learning-service or GenAI. This keeps service-level JWT validation
active across internal calls.

## Local Docker Configuration

Local Docker configuration is in [`docker-compose.yml`](../docker-compose.yml).

Default local URLs:

```text
Frontend:      http://localhost:3000
Keycloak UI:   http://localhost:8090
Realm:         team-drops
Client ID:     team-drops
Issuer:        http://localhost:8090/realms/team-drops
Internal JWKS: http://keycloak:8080/realms/team-drops/protocol/openid-connect/certs
```

The imported local realm is defined in
[`keycloak/realm-export.json`](../keycloak/realm-export.json). It enables:

- realm `team-drops`
- custom login theme `interviewmate`
- public client `team-drops`
- Authorization Code Flow
- PKCE `S256`
- self-registration
- local redirect URIs for `localhost:3000` and `localhost:5173`

The Keycloak login and registration pages use the InterviewMate frontend theme
from
[`helm/team-drops/files/keycloak-theme/interviewmate`](../helm/team-drops/files/keycloak-theme/interviewmate).
Docker Compose mounts this theme into Keycloak for local development, and Helm
packages the same files into a `keycloak-theme` ConfigMap for cluster
deployments.

Local development can still run with authentication disabled:

```powershell
docker compose up --build
```

To test local authentication, enable auth before starting Docker Compose:

```powershell
$env:AUTH_ENABLED="true"
docker compose up --build
```

Then open:

```text
http://localhost:3000
```

## Production Configuration

The Rancher deployment uses
[`helm/team-drops/values-rancher.yaml`](../helm/team-drops/values-rancher.yaml)
for public issuer, internal JWKS, redirect URI, web origin, and frontend
Keycloak URL overrides. Secrets are supplied by GitHub Actions rather than
committed to this file.

Recommended production settings:

```yaml
auth:
  enabled: true

oauth2Proxy:
  enabled: false

keycloak:
  enabled: true
```

Important Helm implementation files:

- [`helm/team-drops/values.yaml`](../helm/team-drops/values.yaml)
- [`helm/team-drops/values-rancher.yaml`](../helm/team-drops/values-rancher.yaml)
- [`helm/team-drops/templates/keycloak.yaml`](../helm/team-drops/templates/keycloak.yaml)
- [`helm/team-drops/templates/oauth2-proxy.yaml`](../helm/team-drops/templates/oauth2-proxy.yaml)
- [`helm/team-drops/templates/ingress.yaml`](../helm/team-drops/templates/ingress.yaml)
- [`helm/team-drops/templates/configmap.yaml`](../helm/team-drops/templates/configmap.yaml)

The current Rancher profile lets backend services validate bearer tokens and
keeps `oauth2-proxy` disabled. The chart can optionally add gateway-level
validation by enabling `oauth2-proxy`:

```text
Browser
-> NGINX Ingress
-> oauth2-proxy /oauth2/auth
-> Keycloak session or Bearer token validation
-> backend service
-> service-level JWT validation
```

Gateway validation does not replace service validation. Backend services still
verify JWTs themselves.

See also: [`docs/nginx-gateway.md`](nginx-gateway.md), especially the gateway
best practice section that explains how NGINX routes and secures browser
traffic while backend integration clients encapsulate learning-service and
GenAI calls.

## Troubleshooting

Invalid redirect URI:

- Check the Keycloak client redirect URIs.
- For local Docker, `keycloak/realm-export.json` must allow
  `http://localhost:3000/*`.
- For production, configure redirect URIs and web origins in Helm values or the
  external Keycloak client.

`401` after login:

- Check that the token issuer matches the configured issuer URI.
- In local Docker, the issuer should be
  `http://localhost:8090/realms/team-drops`.
- Check that services can reach the JWKS URL. In local Docker, services should
  use `http://keycloak:8080/realms/team-drops/protocol/openid-connect/certs`.

Missing Bearer token:

- Check `frontend/src/api/client.js` is calling `getValidAccessToken()`.
- Check the user is authenticated in Keycloak.
- Check the request has `Authorization: Bearer <token>`.

User exists in Keycloak but not in the app database:

- The app user is created only after `GET /user-service/api/v1/users/me`.
- Check that `/users/me` succeeds with a valid Bearer token.
- Check `UserService.getOrCreateOidcUser(...)` can read `sub`, `email`, and
  `name` or `preferred_username` claims from the JWT.
