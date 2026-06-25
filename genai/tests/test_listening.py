from unittest.mock import AsyncMock, patch

from langchain_core.runnables import RunnableLambda

from app.schemas.listening import (
    _OptionLLM,
    _QuestionLLM,
    _QuestionsLLMOutput,
    _ScriptLLMOutput,
)

_FAKE_SCRIPT = (
    "Zwei Freunde treffen sich im Park. Es ist ein schöner Tag und sie sprechen über ihre Arbeit. "
    "Anna arbeitet als Ärztin in einem Krankenhaus. Ihr Freund Thomas ist Lehrer an einer Grundschule."
)

_FOUR_OPTIONS = [
    _OptionLLM(text="Im Park", is_correct=True),
    _OptionLLM(text="Im Büro", is_correct=False),
    _OptionLLM(text="Zu Hause", is_correct=False),
    _OptionLLM(text="Im Café", is_correct=False),
]

# Exactly 4 questions — matches _DEFAULT_QUESTION_COUNT in the router
_FAKE_QUESTIONS_OUTPUT = _QuestionsLLMOutput(
    questions=[
        _QuestionLLM(
            question="Wo treffen sich die zwei Freunde?",
            options=_FOUR_OPTIONS,
            explanation="The text says 'treffen sich im Park'.",
        ),
        _QuestionLLM(
            question="Was ist Annas Beruf?",
            options=[
                _OptionLLM(text="Lehrerin", is_correct=False),
                _OptionLLM(text="Ärztin", is_correct=True),
                _OptionLLM(text="Ingenieurin", is_correct=False),
                _OptionLLM(text="Köchin", is_correct=False),
            ],
            explanation="The text states 'Anna arbeitet als Ärztin'.",
        ),
        _QuestionLLM(
            question="Was ist Thomas' Beruf?",
            options=[
                _OptionLLM(text="Arzt", is_correct=False),
                _OptionLLM(text="Ingenieur", is_correct=False),
                _OptionLLM(text="Lehrer", is_correct=True),
                _OptionLLM(text="Koch", is_correct=False),
            ],
            explanation="The text states 'Thomas ist Lehrer'.",
        ),
        _QuestionLLM(
            question="Wo arbeitet Anna?",
            options=[
                _OptionLLM(text="In einer Schule", is_correct=False),
                _OptionLLM(text="In einem Büro", is_correct=False),
                _OptionLLM(text="In einem Krankenhaus", is_correct=True),
                _OptionLLM(text="Im Park", is_correct=False),
            ],
            explanation="The text says 'in einem Krankenhaus'.",
        ),
    ]
)

_FAKE_AUDIO_B64 = "UklGRiQAAABXQVZFZm10IBAAAA=="

_REQUEST_BODY = {"target_language": "German", "level": "B1", "topic": "friendship"}
_REQUEST_BODY_NO_TOPIC = {"target_language": "German", "level": "B1"}


def _make_two_call_llm(script_result=None, questions_result=None):
    """Return a side_effect function for patching get_structured_llm.

    Routes by schema class name so the mock stays correct if the router ever
    reorders its LLM calls.
    """
    s_result = script_result or _ScriptLLMOutput(script=_FAKE_SCRIPT)
    q_result = questions_result or _FAKE_QUESTIONS_OUTPUT

    def side_effect(schema):
        if schema.__name__ == "_ScriptLLMOutput":
            return RunnableLambda(lambda _: s_result)
        return RunnableLambda(lambda _: q_result)

    return side_effect


def _post_listening(client, tts_enabled=True, body=None, mock_llm=None):
    payload = body if body is not None else _REQUEST_BODY
    side_effect = mock_llm or _make_two_call_llm()
    with (
        patch("app.routers.listening.synthesize", new=AsyncMock(return_value=_FAKE_AUDIO_B64)),
        patch("app.routers.listening.get_structured_llm", side_effect=side_effect),
    ):
        response = client.post("/api/v1/genai/listening/generate", json=payload)
    return response


# ---------------------------------------------------------------------------
# Response structure
# ---------------------------------------------------------------------------


def test_generate_listening_returns_all_fields(client):
    response = _post_listening(client)

    assert response.status_code == 200
    body = response.json()
    assert "script" in body
    assert "questions" in body
    assert "script_audio_b64" in body


def test_generate_listening_script_content(client):
    response = _post_listening(client)

    assert response.json()["script"] == _FAKE_SCRIPT


def test_generate_listening_question_count_matches_default(client):
    """Router must request and return _DEFAULT_QUESTION_COUNT (4) questions."""
    response = _post_listening(client)

    assert len(response.json()["questions"]) == 4


def test_generate_listening_question_structure(client):
    response = _post_listening(client)

    for q in response.json()["questions"]:
        assert "question" in q
        assert "options" in q
        assert "explanation" in q
        assert len(q["options"]) == 4
        for opt in q["options"]:
            assert "text" in opt
            assert "is_correct" in opt


def test_generate_listening_exactly_one_correct_option_per_question(client):
    response = _post_listening(client)

    for q in response.json()["questions"]:
        correct = [o for o in q["options"] if o["is_correct"]]
        assert len(correct) == 1


# ---------------------------------------------------------------------------
# TTS behaviour
# ---------------------------------------------------------------------------


def test_generate_listening_includes_audio_when_tts_enabled(client):
    response = _post_listening(client, tts_enabled=True)

    assert response.json()["script_audio_b64"] == _FAKE_AUDIO_B64


def test_generate_listening_includes_audio_even_when_tts_setting_disabled(client):
    response = _post_listening(client, tts_enabled=False)

    assert response.json()["script_audio_b64"] == _FAKE_AUDIO_B64


def test_generate_listening_tts_failure_returns_bad_gateway(client):
    """A listening exercise is not useful without audio."""
    with (
        patch("app.routers.listening.synthesize", new=AsyncMock(side_effect=RuntimeError("TTS down"))),
        patch("app.routers.listening.get_structured_llm", side_effect=_make_two_call_llm()),
    ):
        response = client.post("/api/v1/genai/listening/generate", json=_REQUEST_BODY)

    assert response.status_code == 502
    assert "TTS synthesis failed" in response.json()["message"]


# ---------------------------------------------------------------------------
# Error cases
# ---------------------------------------------------------------------------


def _raise_llm_error(_):
    raise RuntimeError("LLM down")


def _raise_questions_error(_):
    raise RuntimeError("Questions LLM down")


def test_generate_listening_script_llm_failure_returns_502(client):
    with patch("app.routers.listening.get_structured_llm", return_value=RunnableLambda(_raise_llm_error)):
        response = client.post("/api/v1/genai/listening/generate", json=_REQUEST_BODY)

    assert response.status_code == 502
    assert "LLM" in response.json()["message"]


def test_generate_listening_questions_llm_failure_returns_502(client):
    """Script generation succeeds but questions LLM fails — must return 502."""

    def side_effect(schema):
        if schema.__name__ == "_ScriptLLMOutput":
            return RunnableLambda(lambda _: _ScriptLLMOutput(script=_FAKE_SCRIPT))
        return RunnableLambda(_raise_questions_error)

    with (
        patch("app.routers.listening.synthesize", new=AsyncMock(return_value=_FAKE_AUDIO_B64)),
        patch("app.routers.listening.get_structured_llm", side_effect=side_effect),
    ):
        response = client.post("/api/v1/genai/listening/generate", json=_REQUEST_BODY)

    assert response.status_code == 502


def test_generate_listening_wrong_option_count_returns_502(client):
    """LLM returning the wrong number of options must produce 502, not 500.

    model_construct bypasses Pydantic validation to simulate a malformed LLM
    response (e.g. structured-output parser bug or future schema loosening).
    """
    bad_question = _QuestionLLM.model_construct(
        question="Frage?",
        options=[
            _OptionLLM(text="A", is_correct=True),
            _OptionLLM(text="B", is_correct=False),
            _OptionLLM(text="C", is_correct=False),
            # only 3 options — violates ListeningQuestion.options min_length=4
        ],
        explanation="...",
    )
    bad_questions = _QuestionsLLMOutput.model_construct(questions=[bad_question])

    side_effect = _make_two_call_llm(questions_result=bad_questions)

    with (
        patch("app.routers.listening.synthesize", new=AsyncMock(return_value=_FAKE_AUDIO_B64)),
        patch("app.routers.listening.get_structured_llm", side_effect=side_effect),
    ):
        response = client.post("/api/v1/genai/listening/generate", json=_REQUEST_BODY)

    assert response.status_code == 502


def test_generate_listening_missing_required_fields_returns_422(client):
    response = client.post(
        "/api/v1/genai/listening/generate",
        json={"target_language": "German"},  # level is missing
    )
    assert response.status_code == 422


def test_generate_listening_invalid_level_returns_422(client):
    response = client.post(
        "/api/v1/genai/listening/generate",
        json={"target_language": "German", "level": "Z9"},
    )
    assert response.status_code == 422


# ---------------------------------------------------------------------------
# Optional topic field
# ---------------------------------------------------------------------------


def test_generate_listening_without_topic(client):
    """topic is optional — the endpoint must work when it is omitted."""
    response = _post_listening(client, body=_REQUEST_BODY_NO_TOPIC)

    assert response.status_code == 200
    assert response.json()["script"] == _FAKE_SCRIPT
