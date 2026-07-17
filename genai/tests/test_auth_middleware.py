from unittest.mock import patch

import jwt

from app.config import settings


def test_auth_middleware_allows_public_health_when_enabled(client, monkeypatch):
    monkeypatch.setattr(settings, "auth_enabled", True)

    response = client.get("/health")

    assert response.status_code != 401


def test_auth_middleware_allows_public_liveness_when_enabled(client, monkeypatch):
    monkeypatch.setattr(settings, "auth_enabled", True)

    response = client.get("/live")

    assert response.status_code == 200


def test_auth_middleware_allows_metrics_when_enabled(client, monkeypatch):
    monkeypatch.setattr(settings, "auth_enabled", True)

    response = client.get("/metrics")

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/plain")
    assert (
        'application_info{service="genai-service",version="unknown"} 1.0'
        in response.text
    )


def test_auth_middleware_rejects_missing_bearer_token(client, monkeypatch):
    monkeypatch.setattr(settings, "auth_enabled", True)

    response = client.post(
        "/api/v1/genai/rag/query", json={"topic": "job interview", "question": "Hello?"}
    )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_auth_middleware_rejects_invalid_bearer_token(client, monkeypatch):
    monkeypatch.setattr(settings, "auth_enabled", True)

    with patch("app.middleware.auth._verify_token", side_effect=jwt.InvalidTokenError):
        response = client.post(
            "/api/v1/genai/rag/query",
            headers={"Authorization": "Bearer invalid-token"},
            json={"topic": "job interview", "question": "Hello?"},
        )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_auth_middleware_accepts_valid_bearer_token(client, monkeypatch):
    monkeypatch.setattr(settings, "auth_enabled", True)

    with patch("app.middleware.auth._verify_token", return_value=None):
        response = client.get(
            "/api/v1/genai/rag/topics", headers={"Authorization": "Bearer valid-token"}
        )

    assert response.status_code == 200
