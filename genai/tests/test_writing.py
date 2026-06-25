from unittest.mock import patch

from app.prompts.writing import writing_prompt
from tests.conftest import make_mock_structured_llm
from app.schemas.writing import MAX_DATABASE_TEXT_LENGTH, WritingEvaluationResponse

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
    with patch(
        "app.routers.writing.get_structured_llm", return_value=make_mock_structured_llm(_LLM_RESPONSE)
    ):
        response = client.post("/api/v1/genai/writing/evaluate", json=_REQUEST)

    assert response.status_code == 200
    body = response.json()
    assert body["score"] == 8.0
    assert body["is_correct"] is True
    assert "weak_area" in body
    assert "corrected_answer" in body
    assert "message" in body


def test_evaluate_writing_score_within_range(client):
    with patch(
        "app.routers.writing.get_structured_llm", return_value=make_mock_structured_llm(_LLM_RESPONSE)
    ):
        response = client.post("/api/v1/genai/writing/evaluate", json=_REQUEST)

    score = response.json()["score"]
    assert 0.0 <= score <= 10.0


def test_evaluate_writing_shortens_database_text_fields(client):
    long_text = "x" * 400
    llm_response = WritingEvaluationResponse(
        score=5.0,
        is_correct=False,
        message=long_text,
        weak_area="word order",
        corrected_answer=long_text,
    )

    with patch("app.routers.writing.get_structured_llm", return_value=make_mock_structured_llm(llm_response)):
        response = client.post("/api/v1/genai/writing/evaluate", json=_REQUEST)

    assert response.status_code == 200
    body = response.json()
    assert len(body["message"]) <= MAX_DATABASE_TEXT_LENGTH
    assert len(body["corrected_answer"]) <= MAX_DATABASE_TEXT_LENGTH


def test_evaluate_writing_missing_required_fields_returns_422(client):
    response = client.post("/api/v1/genai/writing/evaluate", json={})
    assert response.status_code == 422


def test_writing_prompt_keeps_correction_in_target_language():
    messages = writing_prompt.format_messages(
        level="A2",
        exercise_type="translation",
        question="Translate: I would like a coffee, please",
        expected_answer="Ich hätte gerne einen Kaffee, bitte.",
        user_answer="Ich mochte ein Kaffee bitte.",
        target_language="German",
    )

    prompt_text = "\n".join(message.content for message in messages)

    assert "corrected_answer must be" in prompt_text
    assert "in German" in prompt_text
    assert "not an English translation" in prompt_text
    assert "Do not penalize" in prompt_text
