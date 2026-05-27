import json
import logging

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.config import settings
from app.llm import get_llm
from app.prompts.corrections import corrections_prompt
from app.prompts.practice import practice_prompt
from app.schemas.practice import (
    ConversationMessage,
    SpeakingPracticeResponse,
    TurnCorrection,
    _ConversationTurnLLM,
    _SessionCorrectionsLLM,
)
from app.stt import transcribe
from app.tts import synthesize

router = APIRouter(prefix="/speaking", tags=["speaking"])
logger = logging.getLogger(__name__)

_MAX_AUDIO_BYTES = 25 * 1024 * 1024  # 25 MB


def _format_history(history: list[ConversationMessage]) -> str:
    if not history:
        return "(no previous turns)"
    return "\n".join(f"{m.role.capitalize()}: {m.text}" for m in history)


def _build_corrections_summary(corrections: list[TurnCorrection]) -> str:
    parts = []
    for c in corrections:
        parts.append(f"You said: {c.original}. It should be: {c.corrected}. {c.explanation}.")
    return " ".join(parts)


@router.post(
    "/practice",
    operation_id="speakingPractice",
    response_model=SpeakingPracticeResponse,
    summary="Conversational speaking practice",
    description=(
        "One turn of a free-form spoken conversation with an AI character in the target language. "
        "The frontend maintains the conversation history and sends it with each request. "
        "On the final turn (end_session=true) the service additionally returns a list of "
        "language corrections made across the full session, plus a spoken audio summary."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def speaking_practice(
    audio: UploadFile = File(..., description="Audio of the learner's spoken turn"),
    scenario: str = Form(..., examples=["waiter at a German restaurant"]),
    target_language: str = Form(..., examples=["German"]),
    level: str = Form(..., examples=["A2"]),
    history_json: str = Form(
        default="[]",
        description="JSON-serialised list of ConversationMessage objects from previous turns",
    ),
    end_session: bool = Form(
        default=False,
        description="Set to true on the final turn to trigger end-of-session corrections",
    ),
) -> SpeakingPracticeResponse:
    audio_bytes = await audio.read()
    if len(audio_bytes) > _MAX_AUDIO_BYTES:
        raise HTTPException(
            status_code=413,
            detail=f"Audio file exceeds {_MAX_AUDIO_BYTES // (1024 * 1024)} MB limit",
        )

    try:
        transcription = await transcribe(audio_bytes, target_language)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"STT transcription failed: {exc}") from exc

    try:
        history = [ConversationMessage(**m) for m in json.loads(history_json)]
    except Exception as exc:
        raise HTTPException(status_code=422, detail=f"Invalid history_json: {exc}") from exc

    llm = get_llm()

    # --- Conversation turn ---
    conv_chain = practice_prompt | llm.with_structured_output(_ConversationTurnLLM)
    try:
        conv_result: _ConversationTurnLLM = await conv_chain.ainvoke(
            {
                "scenario": scenario,
                "target_language": target_language,
                "level": level,
                "history": _format_history(history),
                "transcription": transcription,
            }
        )
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"LLM invocation failed: {exc}") from exc

    ai_audio: str | None = None
    if settings.tts_enabled:
        try:
            ai_audio = await synthesize(conv_result.ai_response, target_language)
        except Exception as exc:
            logger.warning("TTS failed for AI reply: %s", exc)

    # --- End-of-session corrections ---
    corrections: list[TurnCorrection] | None = None
    corrections_audio: str | None = None

    if end_session:
        full_history = history + [
            ConversationMessage(role="user", text=transcription),
            ConversationMessage(role="assistant", text=conv_result.ai_response),
        ]
        history_json_full = json.dumps(
            [m.model_dump() for m in full_history], ensure_ascii=False
        )

        corr_chain = corrections_prompt | llm.with_structured_output(_SessionCorrectionsLLM)
        try:
            corr_result: _SessionCorrectionsLLM = await corr_chain.ainvoke(
                {
                    "target_language": target_language,
                    "level": level,
                    "scenario": scenario,
                    "history_json": history_json_full,
                }
            )
            corrections = corr_result.corrections
        except Exception as exc:
            logger.warning("Corrections LLM call failed, returning without corrections: %s", exc)

        if settings.tts_enabled and corrections:
            try:
                corrections_audio = await synthesize(
                    _build_corrections_summary(corrections), target_language
                )
            except Exception as exc:
                logger.warning("TTS failed for corrections summary: %s", exc)

    return SpeakingPracticeResponse(
        transcription=transcription,
        ai_response_text=conv_result.ai_response,
        ai_response_audio_b64=ai_audio,
        session_corrections=corrections,
        corrections_audio_b64=corrections_audio,
    )
