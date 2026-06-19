from unittest.mock import AsyncMock, MagicMock, patch

from langchain_core.runnables import RunnableLambda

from tests.conftest import make_mock_structured_llm
from app.schemas.speaking import _SpeakingEvaluationLLMOutput

_LLM_RESPONSE = _SpeakingEvaluationLLMOutput(
    score=6.5,
    is_correct=False,
    message=(
        "Good attempt! You used 'an' (contact with a vertical surface) instead of 'auf' "
        "(resting on a horizontal surface). Also check the dative case: 'dem Tisch' not 'den Tisch'."
    ),
    weak_area="grammar",
    corrected_answer="Die Katze ist auf dem Tisch",
)

_FORM_DATA = {
    "exercise_type": "translation",
    "question": "Translate: 'The cat is on the table'",
    "expected_answer": "Die Katze ist auf dem Tisch",
    "target_language": "German",
    "level": "A2",
}

_FAKE_TRANSCRIPTION = "Die Katze ist an den Tisch"
_FAKE_AUDIO_B64 = "UklGRiQAAABXQVZFZm10IBAAAA=="  # minimal valid base64


def _post_speaking(client, tts_enabled=True):
    with (
        patch("app.routers.speaking.transcribe", new=AsyncMock(return_value=_FAKE_TRANSCRIPTION)),
        patch("app.routers.speaking.synthesize", new=AsyncMock(return_value=_FAKE_AUDIO_B64)),
        patch("app.routers.speaking.get_structured_llm", return_value=make_mock_structured_llm(_LLM_RESPONSE)),
        patch("app.routers.speaking.settings") as mock_settings,
    ):
        mock_settings.tts_enabled = tts_enabled
        response = client.post(
            "/api/v1/genai/speaking/evaluate",
            data=_FORM_DATA,
            files={"audio": ("test.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
        )
    return response


def test_evaluate_speaking_returns_all_fields(client):
    response = _post_speaking(client)

    assert response.status_code == 200
    body = response.json()
    assert body["transcription"] == _FAKE_TRANSCRIPTION
    assert "score" in body
    assert "is_correct" in body
    assert "message" in body
    assert "weak_area" in body
    assert "corrected_answer" in body
    assert "feedback_audio_b64" in body


def test_evaluate_speaking_score_within_range(client):
    response = _post_speaking(client)

    score = response.json()["score"]
    assert 0.0 <= score <= 10.0


def test_evaluate_speaking_includes_audio_when_tts_enabled(client):
    response = _post_speaking(client, tts_enabled=True)

    assert response.json()["feedback_audio_b64"] == _FAKE_AUDIO_B64


def test_evaluate_speaking_no_audio_when_tts_disabled(client):
    response = _post_speaking(client, tts_enabled=False)

    assert response.json()["feedback_audio_b64"] is None


def test_evaluate_speaking_missing_audio_returns_422(client):
    response = client.post(
        "/api/v1/genai/speaking/evaluate",
        data=_FORM_DATA,
        # no audio file
    )
    assert response.status_code == 422


def test_evaluate_speaking_stt_failure_returns_502(client):
    with (
        patch("app.routers.speaking.transcribe", new=AsyncMock(side_effect=RuntimeError("STT down"))),
        patch("app.routers.speaking.settings") as mock_settings,
    ):
        mock_settings.tts_enabled = False
        response = client.post(
            "/api/v1/genai/speaking/evaluate",
            data=_FORM_DATA,
            files={"audio": ("test.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
        )
    assert response.status_code == 502
    assert "STT" in response.json()["message"]


def test_evaluate_speaking_llm_failure_returns_502(client):
    with (
        patch("app.routers.speaking.transcribe", new=AsyncMock(return_value=_FAKE_TRANSCRIPTION)),
        patch("app.routers.speaking.get_structured_llm", return_value=RunnableLambda(
            lambda _: (_ for _ in ()).throw(RuntimeError("LLM down"))
        )),
        patch("app.routers.speaking.settings") as mock_settings,
    ):
        mock_settings.tts_enabled = False
        response = client.post(
            "/api/v1/genai/speaking/evaluate",
            data=_FORM_DATA,
            files={"audio": ("test.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
        )
    assert response.status_code == 502


def test_evaluate_speaking_oversized_audio_returns_413(client):
    big_audio = b"X" * (25 * 1024 * 1024 + 1)
    response = client.post(
        "/api/v1/genai/speaking/evaluate",
        data=_FORM_DATA,
        files={"audio": ("big.wav", big_audio, "audio/wav")},
    )
    assert response.status_code == 413
