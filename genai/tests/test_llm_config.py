from unittest.mock import MagicMock

from app.llm.client import get_llm, get_structured_llm


def test_health_reports_only_readiness_when_openai_key_missing(client, monkeypatch):
    monkeypatch.setattr("app.config.settings.llm_provider", "openai")
    monkeypatch.setattr("app.config.settings.llm_api_key", "")
    monkeypatch.setattr("app.config.settings.llm_model", "openai/gpt-oss-120b")
    monkeypatch.setattr("app.config.settings.llm_base_url", "https://logos.example/v1")
    get_llm.cache_clear()

    response = client.get("/health")

    assert response.status_code == 503
    assert response.json() == {"status": "degraded"}


def test_live_reports_process_liveness_when_openai_key_missing(client, monkeypatch):
    monkeypatch.setattr("app.config.settings.llm_provider", "openai")
    monkeypatch.setattr("app.config.settings.llm_api_key", "")
    get_llm.cache_clear()

    response = client.get("/live")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_llm_endpoint_returns_503_when_openai_key_missing(client, monkeypatch):
    monkeypatch.setattr("app.config.settings.llm_provider", "openai")
    monkeypatch.setattr("app.config.settings.llm_api_key", "")
    get_llm.cache_clear()

    response = client.post(
        "/api/v1/genai/exercises/generate",
        json={
            "lesson_id": 3,
            "lesson_topic": "Household items",
            "target_language": "German",
            "level": "A2",
            "existing_exercises": [
                {
                    "type": "translation",
                    "question": "Translate: 'The cat is on the table'",
                    "difficulty": "A2",
                    "expected_answer": "Die Katze ist auf dem Tisch",
                }
            ],
            "count": 1,
        },
    )

    assert response.status_code == 503
    assert response.json() == {
        "code": "LLM_NOT_CONFIGURED",
        "message": "LLM provider openai requires LLM_API_KEY.",
    }


def test_get_structured_llm_uses_strict_json_schema_for_official_openai(monkeypatch):
    monkeypatch.setattr("app.config.settings.llm_provider", "openai")
    monkeypatch.setattr("app.config.settings.llm_base_url", "")
    llm = MagicMock()
    llm.with_structured_output.return_value = "structured"
    monkeypatch.setattr("app.llm.client.get_llm", lambda: llm)

    result = get_structured_llm(dict)

    assert result == "structured"
    llm.with_structured_output.assert_called_once_with(
        dict,
        method="json_schema",
        strict=True,
    )


def test_get_structured_llm_uses_non_strict_json_schema_for_openai_compatible_base_url(monkeypatch):
    monkeypatch.setattr("app.config.settings.llm_provider", "openai")
    monkeypatch.setattr("app.config.settings.llm_base_url", "https://logos.example/v1")
    llm = MagicMock()
    llm.with_structured_output.return_value = "structured"
    monkeypatch.setattr("app.llm.client.get_llm", lambda: llm)

    result = get_structured_llm(dict)

    assert result == "structured"
    llm.with_structured_output.assert_called_once_with(dict, method="json_schema")


def test_get_structured_llm_uses_json_schema_without_strict_for_ollama(monkeypatch):
    monkeypatch.setattr("app.config.settings.llm_provider", "ollama")
    llm = MagicMock()
    llm.with_structured_output.return_value = "structured"
    monkeypatch.setattr("app.llm.client.get_llm", lambda: llm)

    result = get_structured_llm(dict)

    assert result == "structured"
    llm.with_structured_output.assert_called_once_with(dict, method="json_schema")
