from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class ConversationMessage(BaseModel):
    """A single turn in the conversation history."""

    role: Literal["user", "assistant"] = Field(
        ..., description="Speaker — 'user' is the learner, 'assistant' is the AI character"
    )
    text: str = Field(..., description="Spoken or generated text for this turn")


class TurnCorrection(BaseModel):
    """A single language error identified across the session."""

    original: str = Field(..., description="What the learner actually said", examples=["Ich möchte ein Kaffee"])
    corrected: str = Field(..., description="What they should have said", examples=["Ich möchte einen Kaffee"])
    explanation: str = Field(
        ...,
        description="Short grammar or vocabulary note",
        examples=["'Kaffee' is masculine, so the accusative article is 'einen' not 'ein'"],
    )


class _ConversationTurnLLM(BaseModel):
    """LLM structured output for a single conversation turn — internal use only."""

    ai_response: str = Field(..., description="AI character's reply in the target language")


class _SessionCorrectionsLLM(BaseModel):
    """LLM structured output for the end-of-session analysis — internal use only."""

    corrections: list[TurnCorrection] = Field(
        default_factory=list,
        description="Meaningful language errors found in the learner's turns; empty list if none",
    )


class SpeakingPracticeResponse(BaseModel):
    """
    Response for one turn of a speaking practice conversation.
    On normal turns only transcription + AI reply are populated.
    On the final turn (end_session=true) session_corrections and corrections_audio_b64
    are also populated.
    """

    transcription: str = Field(
        ..., description="Whisper transcription of the learner's audio for this turn"
    )
    ai_response_text: str = Field(
        ..., description="AI character's reply in the target language"
    )
    ai_response_audio_b64: str | None = Field(
        default=None, description="Base64-encoded WAV of the AI's reply spoken aloud"
    )
    session_corrections: list[TurnCorrection] | None = Field(
        default=None,
        description="Language errors found across the full session — only present on the final turn",
    )
    corrections_audio_b64: str | None = Field(
        default=None,
        description="Base64-encoded WAV of the corrections summary spoken aloud — only on the final turn",
    )
