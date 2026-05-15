from unittest.mock import patch

from tests.conftest import make_mock_llm
from app.schemas.exercises import GenerateExercisesResponse, GeneratedExercise

_EXERCISE = GeneratedExercise(
    lesson_id=3,
    type="translation",
    question="Translate: 'The door is open'",
    difficulty="A2",
    expected_answer="Die Tür ist offen",
)
_LLM_RESPONSE = GenerateExercisesResponse(exercises=[_EXERCISE])


def test_generate_exercises_returns_exercises(client):
    with patch("app.routers.exercises.get_llm", return_value=make_mock_llm(_LLM_RESPONSE)):
        response = client.post("/exercises/generate", json={
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
        })

    assert response.status_code == 200
    body = response.json()
    assert len(body["exercises"]) == 1
    assert body["exercises"][0]["lesson_id"] == 3
    assert body["exercises"][0]["type"] == "translation"


def test_generate_exercises_passes_lesson_id_through(client):
    """lesson_id from the request must be stamped onto every generated exercise."""
    exercise_without_id = GeneratedExercise(
        lesson_id=0,
        type="fill-in-the-blank",
        question="Die ___ ist offen.",
        difficulty="A2",
        expected_answer="Tür",
    )
    llm_response = GenerateExercisesResponse(exercises=[exercise_without_id])

    with patch("app.routers.exercises.get_llm", return_value=make_mock_llm(llm_response)):
        response = client.post("/exercises/generate", json={
            "lesson_id": 99,
            "lesson_topic": "Household items",
            "target_language": "German",
            "level": "A2",
            "existing_exercises": [
                {
                    "type": "fill-in-the-blank",
                    "question": "Die ___ ist offen.",
                    "difficulty": "A2",
                }
            ],
        })

    assert response.status_code == 200
    for exercise in response.json()["exercises"]:
        assert exercise["lesson_id"] == 99


def test_generate_exercises_missing_required_fields_returns_422(client):
    response = client.post("/exercises/generate", json={})
    assert response.status_code == 422
