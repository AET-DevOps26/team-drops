from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
import jwt
from jwt import PyJWKClient
from jwt.exceptions import InvalidTokenError, PyJWKClientError

from app.config import settings


_PUBLIC_PATHS = ("/health", "/live", "/metrics", "/docs", "/redoc", "/openapi.json")
_jwk_client: PyJWKClient | None = None


def _jwks_uri() -> str:
    if settings.keycloak_jwks_uri:
        return settings.keycloak_jwks_uri
    if settings.keycloak_issuer_uri:
        return (
            settings.keycloak_issuer_uri.rstrip("/") + "/protocol/openid-connect/certs"
        )
    return ""


def _get_jwk_client() -> PyJWKClient:
    global _jwk_client
    if _jwk_client is None:
        jwks_uri = _jwks_uri()
        if not jwks_uri:
            raise InvalidTokenError("JWKS URI is not configured")
        _jwk_client = PyJWKClient(jwks_uri)
    return _jwk_client


def _error(status_code: int, code: str, message: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={"code": code, "message": message, "details": ""},
    )


def _is_public_path(path: str) -> bool:
    return (
        path in _PUBLIC_PATHS or path.startswith("/docs/") or path.startswith("/redoc/")
    )


def _verify_token(token: str) -> None:
    signing_key = _get_jwk_client().get_signing_key_from_jwt(token)
    decode_options = {"verify_aud": bool(settings.keycloak_audience)}
    decode_kwargs = {
        "key": signing_key.key,
        "algorithms": ["RS256"],
        "options": decode_options,
    }

    if settings.keycloak_issuer_uri:
        decode_kwargs["issuer"] = settings.keycloak_issuer_uri.rstrip("/")
    if settings.keycloak_audience:
        decode_kwargs["audience"] = settings.keycloak_audience

    jwt.decode(token, **decode_kwargs)


def add_auth_middleware(app: FastAPI) -> None:
    @app.middleware("http")
    async def jwt_auth(request: Request, call_next):
        if not settings.auth_enabled or _is_public_path(request.url.path):
            return await call_next(request)

        authorization = request.headers.get("Authorization", "")
        scheme, _, token = authorization.partition(" ")

        if scheme.lower() != "bearer" or not token:
            return _error(401, "UNAUTHORIZED", "Missing Bearer token")

        try:
            _verify_token(token)
        except (InvalidTokenError, PyJWKClientError):
            return _error(401, "UNAUTHORIZED", "Invalid Bearer token")

        return await call_next(request)
