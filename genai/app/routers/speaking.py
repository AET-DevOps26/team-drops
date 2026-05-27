import logging

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

logger = logging.getLogger(__name__)

from app.config import settings
from app.llm import get_llm
from app.prompts.speaking import speaking_prompt
from app.schemas.speaking import SpeakingEvaluationResponse, _SpeakingEvaluationLLMOutput
from app.stt import transcribe
from app.tts import synthesize

router = APIRouter(prefix="/speaking", tags=["speaking"])

_MAX_AUDIO_BYTES = 25 * 1024 * 1024  # 25 MB


@router.post(
    "/evaluate",
    operation_id="evaluateSpeaking",
    response_model=SpeakingEvaluationResponse,
    summary="Evaluate a spoken answer",
    description=(
        "Transcribe the learner's audio with Whisper, then evaluate the transcription "
        "against the expected answer using the configured LLM. "
        "Returns a numeric score (0–10), feedback, the corrected answer, and optionally "
        "a base64-encoded WAV of the corrected answer spoken aloud (when TTS is enabled)."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def evaluate_speaking(
    audio: UploadFile = File(..., description="Audio recording of the learner's answer (WAV, MP3, WebM, etc.)"),
    user_id: int = Form(...),
    exercise_id: int = Form(...),
    exercise_type: str = Form(..., examples=["translation"]),
    question: str = Form(..., examples=["Translate: 'The cat is on the table'"]),
    expected_answer: str = Form(..., examples=["Die Katze ist auf dem Tisch"]),
    target_language: str = Form(..., examples=["German"]),
    level: str = Form(..., examples=["A2"]),
) -> SpeakingEvaluationResponse:
    audio_bytes = await audio.read()
    if len(audio_bytes) > _MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail=f"Audio file exceeds {_MAX_AUDIO_BYTES // (1024 * 1024)} MB limit")

    try:
        transcription = await transcribe(audio_bytes, target_language)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"STT transcription failed: {exc}") from exc

    llm = get_llm()
    chain = speaking_prompt | llm.with_structured_output(_SpeakingEvaluationLLMOutput)

    try:
        llm_result: _SpeakingEvaluationLLMOutput = await chain.ainvoke(
            {
                "target_language": target_language,
                "level": level,
                "exercise_type": exercise_type,
                "question": question,
                "expected_answer": expected_answer,
                "user_answer": transcription,
            }
        )
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"LLM invocation failed: {exc}") from exc

    feedback_audio: str | None = None
    if settings.tts_enabled:
        try:
            feedback_audio = await synthesize(llm_result.corrected_answer, target_language)
        except Exception as exc:
            # TTS is best-effort — the evaluation already succeeded, so degrade gracefully
            # rather than discarding the score and feedback with a 502.
            logger.warning("TTS synthesis failed, returning evaluation without audio: %s", exc)

    return SpeakingEvaluationResponse(
        transcription=transcription,
        score=llm_result.score,
        is_correct=llm_result.is_correct,
        message=llm_result.message,
        weak_area=llm_result.weak_area,
        corrected_answer=llm_result.corrected_answer,
        feedback_audio_b64=feedback_audio,
    )
