from unittest.mock import patch

from tests.conftest import make_mock_llm
from app.schemas.writing import WritingEvaluationResponse

_LLM_RESPONSE = WritingEvaluationResponse(
    score=8.0,
    is_correct=True,
    message="Good attempt! 'voudrais' needs a conditional ending (-ais not -ai), and French requires accents: 'café' not 'cafe'.",
    weak_area="accents and diacritics",
    corrected_answer="Je voudrais un café, s'il vous plaît",
)

_REQUEST = {
    "user_id": 42,
    "exercise_id": 7,
    "exercise_type": "translation",
    "question": "Translate: 'I would like a coffee, please'",
    "expected_answer": "Je voudrais un café, s'il vous plaît",
    "user_answer": "Je voudrai un cafe s'il vous plait",
    "target_language": "French",
    "level": "A2",
}


def test_evaluate_writing_returns_score_and_feedback(client):
    with patch("app.routers.writing.get_llm", return_value=make_mock_llm(_LLM_RESPONSE)):
        response = client.post("/writing/evaluate", json=_REQUEST)

    assert response.status_code == 200
    body = response.json()
    assert body["score"] == 8.0
    assert body["is_correct"] is True
    assert "weak_area" in body
    assert "corrected_answer" in body
    assert "message" in body


def test_evaluate_writing_score_within_range(client):
    with patch("app.routers.writing.get_llm", return_value=make_mock_llm(_LLM_RESPONSE)):
        response = client.post("/writing/evaluate", json=_REQUEST)

    score = response.json()["score"]
    assert 0.0 <= score <= 10.0


def test_evaluate_writing_missing_required_fields_returns_422(client):
    response = client.post("/writing/evaluate", json={})
    assert response.status_code == 422
