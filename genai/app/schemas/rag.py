from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class RagQueryRequest(BaseModel):
    topic: str = Field(
        ...,
        description="Folder name under RAG doc DB, e.g. 'job interview'",
        examples=["job interview"],
    )
    question: str = Field(
        ...,
        min_length=1,
        description="Question to answer from the topic corpus",
        examples=["How should I answer behavioral interview questions?"],
    )
    top_k: int = Field(
        default=5,
        ge=1,
        le=12,
        description="Number of retrieved chunks to pass to the LLM",
    )
    rebuild_corpus: bool = Field(
        default=False,
        description="Force rebuilding corpus files before retrieval",
    )


class RagSource(BaseModel):
    source: str
    page: int
    chunk_index: int
    score: float
    text: str


class RagQueryResponse(BaseModel):
    topic: str
    question: str
    answer: str
    sources: list[RagSource]


class RagCorpusResponse(BaseModel):
    topic: str
    pdf_count: int
    chunk_count: int
    corpus_dir: str


class RagTopicsResponse(BaseModel):
    topics: list[str]


class RagLearningPlanRequest(BaseModel):
    topic: str = Field(
        ...,
        description="Folder name under RAG doc DB used as the grounding corpus",
        examples=["job interview"],
    )
    learning_goal: str = Field(
        ...,
        min_length=1,
        description="Learner's goal for the generated learning plan",
        examples=["Prepare for a German job interview"],
    )
    target_language: str = Field(
        ...,
        min_length=1,
        description="Language the plan should teach and use for learner-facing content",
        examples=["German"],
    )
    level: Literal["A1", "A2", "B1", "B2", "C1", "C2"] = Field(
        ...,
        description="Learner CEFR level",
        examples=["B1"],
    )
    duration_weeks: int = Field(..., ge=1, le=52)
    study_hours_per_week: int = Field(..., ge=1, le=80)
    minimum_lessons: int = Field(..., ge=1, le=24)
    maximum_lessons: int = Field(..., ge=1, le=24)
    exercise_types: list[str] = Field(
        ...,
        min_length=1,
        description="Learning-service-compatible exercise type names to include",
        examples=[["writing", "speaking"]],
    )
    top_k: int = Field(default=6, ge=1, le=12)
    rebuild_corpus: bool = Field(default=False)


class RagLearningPlanExercise(BaseModel):
    type: str = Field(..., description="Learning-service ExerciseType enum value")
    subtype: str = Field(..., description="Learning-service ExerciseSubtype enum value")
    question: str
    expected_answer: str
    difficulty: str


class RagLearningPlanLesson(BaseModel):
    title: str
    topic: str
    summary: str
    order_number: int = Field(..., ge=1)
    content_blocks: list[str] = Field(default_factory=list)
    exercises: list[RagLearningPlanExercise] = Field(..., min_length=1)


class RagLearningPlanResponse(BaseModel):
    title: str
    description: str
    goal: str
    language: str
    level: str
    duration: str
    lessons: list[RagLearningPlanLesson] = Field(..., min_length=1)
    sources: list[RagSource] = Field(default_factory=list)
