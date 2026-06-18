import asyncio
import logging

from fastapi import APIRouter, HTTPException

from app.config import settings
from app.llm import get_structured_llm
from app.prompts.listening import listening_questions_prompt, listening_script_prompt
from app.schemas.listening import (
    ListeningGenerateRequest,
    ListeningGenerateResponse,
    ListeningOption,
    ListeningQuestion,
    _QuestionsLLMOutput,
    _ScriptLLMOutput,
)
from app.tts import synthesize

router = APIRouter(prefix="/listening", tags=["listening"])
logger = logging.getLogger(__name__)

_DEFAULT_QUESTION_COUNT = 4


@router.post(
    "/generate",
    operation_id="generateListening",
    response_model=ListeningGenerateResponse,
    summary="Generate a listening comprehension exercise",
    description=(
        "Generate a short listening script in the target language at the specified CEFR level. "
        "Once the script is ready, questions and audio are produced concurrently: "
        "the LLM generates multiple-choice comprehension questions while TTS synthesises "
        "the script as spoken audio. "
        "Returns the script text, questions (four options each, exactly one correct), "
        "and an optional base64-encoded WAV audio of the script (when TTS is enabled). "
        "TTS failure is non-fatal — the exercise is returned without audio."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def generate_listening(body: ListeningGenerateRequest) -> ListeningGenerateResponse:
    # Step 1: generate the listening script — questions and TTS both depend on it
    script_chain = listening_script_prompt | get_structured_llm(_ScriptLLMOutput)
    try:
        script_result: _ScriptLLMOutput = await script_chain.ainvoke(
            {
                "target_language": body.target_language,
                "level": body.level,
                "topic": body.topic or "everyday life",
            }
        )
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"LLM script generation failed: {exc}"
        ) from exc

    # Step 2: generate comprehension questions and synthesise audio concurrently —
    # both are independent once the script text is available.
    questions_chain = listening_questions_prompt | get_structured_llm(_QuestionsLLMOutput)

    questions_coro = questions_chain.ainvoke(
        {
            "target_language": body.target_language,
            "level": body.level,
            "script": script_result.script,
            "count": _DEFAULT_QUESTION_COUNT,
        }
    )
    # asyncio.sleep(0) is a no-op coroutine that resolves to None when TTS is off,
    # keeping the gather call symmetric without a purpose-built helper function.
    tts_coro = (
        synthesize(script_result.script, body.target_language)
        if settings.tts_enabled
        else asyncio.sleep(0)
    )

    questions_raw, audio_raw = await asyncio.gather(
        questions_coro, tts_coro, return_exceptions=True
    )

    if isinstance(questions_raw, BaseException):
        raise HTTPException(
            status_code=502,
            detail=f"LLM questions generation failed: {questions_raw}",
        )

    # TTS is best-effort — a synthesis failure must not discard the generated exercise
    script_audio: str | None = None
    if settings.tts_enabled:
        if isinstance(audio_raw, BaseException):
            logger.warning(
                "TTS synthesis failed, returning exercise without audio: %s", audio_raw
            )
        else:
            script_audio = audio_raw

    # Guard against malformed LLM output (e.g. wrong option count) that passes
    # _QuestionsLLMOutput deserialization but violates ListeningQuestion constraints.
    # Without this, a ValidationError here would surface as an unhandled 500.
    try:
        questions = [
            ListeningQuestion(
                question=q.question,
                options=[ListeningOption(text=o.text, is_correct=o.is_correct) for o in q.options],
                explanation=q.explanation,
            )
            for q in questions_raw.questions
        ]
    except Exception as exc:
        raise HTTPException(
            status_code=502,
            detail=f"LLM returned malformed questions (unexpected option count or missing fields): {exc}",
        ) from exc

    return ListeningGenerateResponse(
        script=script_result.script,
        questions=questions,
        script_audio_b64=script_audio,
    )
