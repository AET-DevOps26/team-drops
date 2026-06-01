from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class ListeningGenerateRequest(BaseModel):
    target_language: str = Field(
        ...,
        description="Language the script and questions will be written in",
        examples=["German"],
    )
    level: Literal["A1", "A2", "B1", "B2", "C1", "C2"] = Field(
        ...,
        description="CEFR level of the learner — controls script vocabulary and grammar complexity",
        examples=["B1"],
    )
    topic: str | None = Field(
        default=None,
        description="Optional topic hint for the script (e.g. 'job interview', 'travel'). "
        "Defaults to everyday life when omitted.",
        examples=["job interview"],
    )


class ListeningOption(BaseModel):
    text: str = Field(
        ...,
        description="The option text shown to the learner",
        examples=["Im Park"],
    )
    is_correct: bool = Field(
        ...,
        description="True if this is the correct answer — exactly one option per question is true",
    )


class ListeningQuestion(BaseModel):
    question: str = Field(
        ...,
        description="The question text in the target language",
        examples=["Wo treffen sich die zwei Freunde?"],
    )
    options: list[ListeningOption] = Field(
        ...,
        description="Exactly four answer options — exactly one must have is_correct=true",
        min_length=4,
        max_length=4,
    )
    explanation: str = Field(
        ...,
        description="Why the correct answer is right — shown after the learner submits",
        examples=["The text says 'treffen sich im Park', which means 'they meet in the park'."],
    )


class ListeningGenerateResponse(BaseModel):
    script: str = Field(
        ...,
        description="The listening passage in the target language",
    )
    questions: list[ListeningQuestion] = Field(
        ...,
        description="Multiple-choice comprehension questions about the script",
        min_length=1,
    )
    script_audio_b64: str | None = Field(
        default=None,
        description="Base64-encoded WAV of the script spoken aloud (null when TTS is disabled)",
    )


# ---------------------------------------------------------------------------
# Internal LLM output schemas — not exposed in the public API
# ---------------------------------------------------------------------------


class _ScriptLLMOutput(BaseModel):
    """LLM output schema for the script-generation step."""

    script: str


class _OptionLLM(BaseModel):
    text: str
    is_correct: bool


class _QuestionLLM(BaseModel):
    question: str
    options: list[_OptionLLM] = Field(..., min_length=4, max_length=4)
    explanation: str


class _QuestionsLLMOutput(BaseModel):
    """LLM output schema for the questions-generation step."""

    questions: list[_QuestionLLM]
