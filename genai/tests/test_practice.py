import json
from unittest.mock import AsyncMock, MagicMock, patch

from langchain_core.runnables import RunnableLambda

from tests.conftest import make_mock_llm
from app.schemas.practice import _ConversationTurnLLM, _SessionCorrectionsLLM, TurnCorrection

_CONV_LLM_RESPONSE = _ConversationTurnLLM(
    ai_response="Guten Tag! Was darf ich Ihnen bringen?"
)

_CORRECTIONS_LLM_RESPONSE = _SessionCorrectionsLLM(
    corrections=[
        TurnCorrection(
            original="Ich möchte ein Kaffee",
            corrected="Ich möchte einen Kaffee",
            explanation="'Kaffee' is masculine; accusative article is 'einen'",
        )
    ]
)

_EMPTY_CORRECTIONS = _SessionCorrectionsLLM(corrections=[])

_FAKE_TRANSCRIPTION = "Ich möchte ein Kaffee bitte"
_FAKE_AUDIO_B64 = "UklGRiQAAABXQVZFZm10IBAAAA=="

_BASE_FORM = {
    "scenario": "waiter at a German restaurant",
    "target_language": "German",
    "level": "A2",
    "history_json": "[]",
    "end_session": "false",
}


def _make_dual_mock_llm(conv_response, corr_response):
    """Return a mock LLM that dispatches by schema type, not call order."""
    mock = MagicMock()

    def _with_structured_output(schema):
        if schema is _ConversationTurnLLM:
            return RunnableLambda(lambda _: conv_response)
        return RunnableLambda(lambda _: corr_response)

    mock.with_structured_output.side_effect = _with_structured_output
    return mock


def _post_practice(client, extra_form=None, conv_llm=None, corr_llm=None, tts_enabled=True):
    form = {**_BASE_FORM, **(extra_form or {})}
    mock_llm = _make_dual_mock_llm(
        conv_llm or _CONV_LLM_RESPONSE,
        corr_llm or _CORRECTIONS_LLM_RESPONSE,
    )

    with (
        patch("app.routers.speaking.transcribe", new=AsyncMock(return_value=_FAKE_TRANSCRIPTION)),
        patch("app.routers.speaking.synthesize", new=AsyncMock(return_value=_FAKE_AUDIO_B64)),
        patch("app.routers.speaking.get_llm", return_value=mock_llm),
        patch("app.routers.speaking.settings") as mock_settings,
    ):
        mock_settings.tts_enabled = tts_enabled
        response = client.post(
            "/api/v1/genai/speaking/practice",
            data=form,
            files={"audio": ("test.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
        )
    return response


def test_practice_returns_transcription_and_ai_reply(client):
    response = _post_practice(client)

    assert response.status_code == 200
    body = response.json()
    assert body["transcription"] == _FAKE_TRANSCRIPTION
    assert body["ai_response_text"] == _CONV_LLM_RESPONSE.ai_response
    assert body["session_corrections"] is None
    assert body["corrections_audio_b64"] is None


def test_practice_ai_reply_audio_present_when_tts_enabled(client):
    response = _post_practice(client, tts_enabled=True)

    assert response.json()["ai_response_audio_b64"] == _FAKE_AUDIO_B64


def test_practice_final_turn_returns_corrections(client):
    response = _post_practice(
        client,
        extra_form={"end_session": "true"},
        corr_llm=_CORRECTIONS_LLM_RESPONSE,
    )

    body = response.json()
    assert body["session_corrections"] is not None
    assert len(body["session_corrections"]) == 1
    assert body["session_corrections"][0]["original"] == "Ich möchte ein Kaffee"
    assert body["corrections_audio_b64"] == _FAKE_AUDIO_B64


def test_practice_no_corrections_when_no_errors(client):
    response = _post_practice(
        client,
        extra_form={"end_session": "true"},
        corr_llm=_EMPTY_CORRECTIONS,
    )

    body = response.json()
    assert body["session_corrections"] == []
    assert body["corrections_audio_b64"] is None  # no audio when nothing to correct


def test_practice_history_is_accepted(client):
    history = [
        {"role": "assistant", "text": "Guten Tag!"},
        {"role": "user", "text": "Hallo, ich möchte bestellen"},
    ]
    response = _post_practice(client, extra_form={"history_json": json.dumps(history)})

    assert response.status_code == 200


def test_practice_invalid_history_returns_422(client):
    with (
        patch("app.routers.speaking.transcribe", new=AsyncMock(return_value=_FAKE_TRANSCRIPTION)),
        patch("app.routers.speaking.get_llm", return_value=make_mock_llm(_CONV_LLM_RESPONSE)),
        patch("app.routers.speaking.settings") as mock_settings,
    ):
        mock_settings.tts_enabled = False
        response = client.post(
            "/api/v1/genai/speaking/practice",
            data={**_BASE_FORM, "history_json": "not valid json"},
            files={"audio": ("test.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
        )
    assert response.status_code == 422


def test_practice_missing_audio_returns_422(client):
    response = client.post("/api/v1/genai/speaking/practice", data=_BASE_FORM)
    assert response.status_code == 422


def test_practice_stt_failure_returns_502(client):
    with (
        patch("app.routers.speaking.transcribe", new=AsyncMock(side_effect=RuntimeError("STT down"))),
        patch("app.routers.speaking.settings") as mock_settings,
    ):
        mock_settings.tts_enabled = False
        response = client.post(
            "/api/v1/genai/speaking/practice",
            data=_BASE_FORM,
            files={"audio": ("test.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
        )
    assert response.status_code == 502
    assert "STT" in response.json()["message"]


def test_practice_llm_failure_returns_502(client):
    with (
        patch("app.routers.speaking.transcribe", new=AsyncMock(return_value=_FAKE_TRANSCRIPTION)),
        patch("app.routers.speaking.get_llm", return_value=make_mock_llm(RuntimeError("LLM down"))),
        patch("app.routers.speaking.settings") as mock_settings,
    ):
        mock_settings.tts_enabled = False
        # make the chain raise instead of return
        failing_llm = MagicMock()
        failing_llm.with_structured_output.return_value = RunnableLambda(
            lambda _: (_ for _ in ()).throw(RuntimeError("LLM down"))
        )
        with patch("app.routers.speaking.get_llm", return_value=failing_llm):
            response = client.post(
                "/api/v1/genai/speaking/practice",
                data=_BASE_FORM,
                files={"audio": ("test.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
            )
    assert response.status_code == 502


def test_practice_oversized_audio_returns_413(client):
    big_audio = b"X" * (25 * 1024 * 1024 + 1)
    response = client.post(
        "/api/v1/genai/speaking/practice",
        data=_BASE_FORM,
        files={"audio": ("big.wav", big_audio, "audio/wav")},
    )
    assert response.status_code == 413


def test_practice_oversized_history_returns_413(client):
    with (
        patch("app.routers.speaking.transcribe", new=AsyncMock(return_value=_FAKE_TRANSCRIPTION)),
        patch("app.routers.speaking.settings") as mock_settings,
    ):
        mock_settings.tts_enabled = False
        response = client.post(
            "/api/v1/genai/speaking/practice",
            data={**_BASE_FORM, "history_json": "x" * (100 * 1024 + 1)},
            files={"audio": ("test.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
        )
    assert response.status_code == 413
