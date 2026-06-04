from app.llm.client import get_llm


def test_health_reports_degraded_when_openai_key_missing(client, monkeypatch):
    monkeypatch.setattr("app.config.settings.llm_provider", "openai")
    monkeypatch.setattr("app.config.settings.llm_api_key", "")
    get_llm.cache_clear()

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "degraded"
    assert response.json()["llm_configured"] is False


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
