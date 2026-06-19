from __future__ import annotations

from pydantic import AliasChoices, BaseModel, ConfigDict, Field, field_validator


MAX_DATABASE_TEXT_LENGTH = 254


def _shorten_for_database(value: str | None) -> str | None:
    if value is None or len(value) <= MAX_DATABASE_TEXT_LENGTH:
        return value

    return value[: MAX_DATABASE_TEXT_LENGTH - 3].rstrip() + "..."


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
        examples=["Je voudrais un cafe, s'il vous plait"],
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

    model_config = ConfigDict(populate_by_name=True)

    score: float = Field(
        ...,
        ge=0.0,
        le=10.0,
        description="Numeric score 0-10, maps to UserAnswer.score",
        examples=[6.5],
    )
    is_correct: bool = Field(
        ...,
        description="True if the answer is substantially correct",
    )
    message: str = Field(
        ...,
        validation_alias=AliasChoices("message", "feedback"),
        max_length=MAX_DATABASE_TEXT_LENGTH,
        description=(
            "Brief feedback including what was wrong and why. Must be shorter "
            "than 255 characters because it maps to Feedback.message."
        ),
        examples=[
            "Good attempt. Use the conditional 'voudrais' and include the accent in 'cafe'."
        ],
    )
    weak_area: str = Field(
        ...,
        description="Short grammatical or lexical category to improve",
        examples=["accents and diacritics"],
    )
    corrected_answer: str = Field(
        ...,
        max_length=MAX_DATABASE_TEXT_LENGTH,
        description="The corrected version of the learner's answer",
        examples=["Je voudrais un cafe, s'il vous plait"],
    )

    @field_validator("message", "corrected_answer", mode="before")
    @classmethod
    def shorten_database_text(cls, value: str) -> str:
        return _shorten_for_database(value)
