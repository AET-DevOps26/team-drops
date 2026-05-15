from __future__ import annotations

from pydantic import BaseModel, Field


class WritingEvaluationRequest(BaseModel):
    """
    Request body for evaluating a learner's written answer against an exercise.
    Maps directly to the UserAnswer + Exercise domain objects in the Java backend.
    """

    user_id: int = Field(..., examples=[42])
    exercise_id: int = Field(..., examples=[7])
    exercise_type: str = Field(
        ...,
        description="Type of the exercise being answered",
        examples=["translation"],
    )
    question: str = Field(
        ...,
        description="The original exercise question shown to the learner",
        examples=["Translate: 'I would like a coffee, please'"],
    )
    expected_answer: str = Field(
        ...,
        description="The canonical correct answer",
        examples=["Je voudrais un café, s'il vous plaît"],
    )
    user_answer: str = Field(
        ...,
        description="The learner's submitted written answer",
        examples=["Je voudrai un cafe s'il vous plait"],
    )
    target_language: str = Field(..., examples=["French"])
    level: str = Field(..., examples=["A2"])


class WritingEvaluationResponse(BaseModel):
    """
    AI evaluation of a written answer.
    score maps to UserAnswer.score; message and weak_area map directly to
    the Feedback entity (Feedback.message, Feedback.weakArea) in the backend.
    """

    score: float = Field(
        ...,
        ge=0.0,
        le=10.0,
        description="Numeric score 0–10 — maps to UserAnswer.score",
        examples=[6.5],
    )
    is_correct: bool = Field(
        ...,
        description="True if the answer is substantially correct",
    )
    message: str = Field(
        ...,
        description=(
            "Full feedback including what was wrong and why — maps to Feedback.message. "
            "Should be a complete, helpful explanation suitable for storing as the feedback record."
        ),
        examples=[
            "Good attempt! 'voudrais' needs a conditional ending (-ais not -ai), and French requires accents: 'café' not 'cafe'. Otherwise the structure is correct."
        ],
    )
    weak_area: str = Field(
        ...,
        description="Short grammatical/lexical category to improve — maps to Feedback.weakArea",
        examples=["accents and diacritics"],
    )
    corrected_answer: str = Field(
        ...,
        description="The corrected version of the learner's answer — for frontend display",
        examples=["Je voudrais un café, s'il vous plaît"],
    )
