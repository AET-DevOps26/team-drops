from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field, model_validator


_RUBRIC_SCORE_FIELDS = (
    "task_completion",
    "grammar",
    "vocabulary",
    "fluency",
    "pronunciation",
)


class SpeakingEvaluationResponse(BaseModel):
    """
    AI evaluation of a spoken answer.
    score and feedback fields mirror WritingEvaluationResponse so both map to
    the same UserAnswer + Feedback domain objects in the backend.
    transcription is the raw Whisper output — useful for displaying what was heard.
    feedback_audio_b64 is a base64-encoded WAV of the corrected answer spoken aloud
    (only present when TTS is enabled).
    """

    transcription: str = Field(
        ...,
        description="Speech-to-text transcription of the learner's audio",
        examples=["Die Katze ist an den Tisch"],
    )
    score: float = Field(
        ...,
        ge=0.0,
        le=10.0,
        description="Numeric score 0–10 — maps to UserAnswer.score",
        examples=[6.5],
    )
    is_correct: bool = Field(
        ...,
        description="True if the spoken answer is substantially correct",
    )
    message: str = Field(
        ...,
        description=(
            "Full feedback covering what was wrong and why — maps to Feedback.message. "
            "Should be a complete, helpful explanation suitable for storing as the feedback record."
        ),
        examples=[
            "Good attempt! You used 'an' (contact/vertical surface) instead of 'auf' (horizontal surface). "
            "Also check the case: 'Tisch' requires the dative 'dem' here."
        ],
    )
    weak_area: str = Field(
        ...,
        description="Short category to improve — maps to Feedback.weakArea",
        examples=["preposition usage"],
    )
    corrected_answer: str = Field(
        ...,
        description="The corrected version of the learner's answer — for frontend display",
        examples=["Die Katze ist auf dem Tisch"],
    )
    feedback_audio_b64: str | None = Field(
        default=None,
        description="Base64-encoded WAV audio of corrected_answer spoken aloud (null when TTS is disabled)",
    )


# LLM fills everything except transcription and feedback_audio_b64 (set by the router).
class _SpeakingEvaluationLLMOutput(BaseModel):
    """Schema used only for LLM structured output — excludes fields set outside the chain."""

    @model_validator(mode="before")
    @classmethod
    def normalize_model_output(cls, data):
        if not isinstance(data, dict):
            return data

        normalized = dict(data)
        if "score" not in normalized and "total_score" in normalized:
            normalized["score"] = normalized["total_score"]
        if "score" not in normalized:
            rubric_scores = [
                normalized.get(field)
                for field in _RUBRIC_SCORE_FIELDS
                if normalized.get(field) is not None
            ]
            if rubric_scores:
                normalized["score"] = max(0.0, min(10.0, sum(float(score) for score in rubric_scores)))
        if "message" not in normalized and "feedback" in normalized:
            normalized["message"] = normalized["feedback"]
        weak_area = str(normalized.get("weak_area", "")).strip().lower().replace("_", " ")
        if weak_area in {"task completion", "task completion and meaning", "meaning", "completeness"}:
            normalized["weak_area"] = "fluency"
        return normalized

    score: float = Field(..., ge=0.0, le=10.0)
    is_correct: bool
    message: str
    weak_area: Literal[
        "pronunciation", "fluency", "vocabulary", "grammar", "word order", "verb conjugation"
    ]
    corrected_answer: str
