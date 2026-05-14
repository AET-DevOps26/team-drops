from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class ExerciseContext(BaseModel):
    """An existing exercise provided as a style/difficulty reference."""

    type: str = Field(
        ...,
        description="Exercise type, e.g. 'translation', 'fill-in-the-blank', 'multiple-choice'",
        examples=["translation"],
    )
    question: str = Field(
        ...,
        description="The exercise prompt shown to the learner",
        examples=["Translate: 'The cat is on the table'"],
    )
    difficulty: str = Field(..., examples=["beginner"])
    expected_answer: str | None = Field(
        default=None,
        examples=["Die Katze ist auf dem Tisch"],
    )


class GenerateExercisesRequest(BaseModel):
    """
    Request body for generating additional exercises similar to existing ones.
    Provide 1–5 existing exercises as style references — the AI will match their
    format, difficulty, and language level when creating new ones.
    """

    lesson_id: int = Field(
        ...,
        description="ID of the lesson these exercises belong to — passed through to each generated exercise",
        examples=[3],
    )
    lesson_topic: str = Field(
        ...,
        description="Topic or theme of the lesson",
        examples=["Everyday household items"],
    )
    target_language: str = Field(
        ...,
        description="Language being learned (full name or BCP-47 tag)",
        examples=["German"],
    )
    level: str = Field(
        ...,
        description="CEFR level or descriptive level of the learner",
        examples=["A2"],
    )
    existing_exercises: list[ExerciseContext] = Field(
        ...,
        description="1–5 existing exercises to use as style and difficulty references",
        min_length=1,
        max_length=5,
    )
    count: int = Field(
        default=3,
        ge=1,
        le=10,
        description="Number of new exercises to generate",
        examples=[3],
    )


class GeneratedExercise(BaseModel):
    """A single AI-generated exercise. All fields map directly to Exercise in the backend."""

    lesson_id: int = Field(..., description="Lesson this exercise belongs to — passed through from the request")
    type: Literal["translation", "fill-in-the-blank", "multiple-choice", "sentence-building"] = Field(
        ...,
        description="Exercise type — maps to Exercise.type",
    )
    question: str = Field(..., description="The exercise prompt — maps to Exercise.question")
    difficulty: str = Field(..., description="Difficulty level — maps to Exercise.difficulty")
    expected_answer: str = Field(..., description="The correct answer — maps to Exercise.expectedAnswer")


class GenerateExercisesResponse(BaseModel):
    """
    Response body containing newly generated exercises.
    Each exercise matches the style and difficulty of the provided reference set.
    """

    exercises: list[GeneratedExercise] = Field(
        ...,
        description="Newly generated exercises in the same style as the provided examples",
    )
