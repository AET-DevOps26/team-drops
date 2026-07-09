from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field, model_validator


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

    @model_validator(mode="before")
    @classmethod
    def normalize_llm_shape(cls, data: Any) -> Any:
        if not isinstance(data, dict):
            return data

        normalized = dict(data)
        if not normalized.get("question"):
            normalized["question"] = normalized.get("prompt") or normalized.get("description")
        if not normalized.get("expected_answer"):
            normalized["expected_answer"] = (
                normalized.get("answer")
                or normalized.get("expectedAnswer")
                or "Open-ended answer grounded in the lesson content."
            )
        if not normalized.get("difficulty"):
            normalized["difficulty"] = normalized.get("level") or "A2"

        subtype = normalized.get("subtype")
        type_for_subtype = {
            "multiple_choice": "reading",
            "listening_choice": "listening",
            "speaking_prompt": "speaking",
            "translation": "writing",
            "fill_in_blank": "writing",
            "sentence_building": "writing",
            "free_text": "writing",
        }
        if subtype in type_for_subtype:
            normalized["type"] = type_for_subtype[subtype]

        return normalized


class RagLearningPlanLesson(BaseModel):
    title: str
    topic: str
    summary: str
    order_number: int = Field(..., ge=1)
    content_blocks: list[str] = Field(default_factory=list)
    exercises: list[RagLearningPlanExercise] = Field(..., min_length=1)

    @model_validator(mode="before")
    @classmethod
    def normalize_llm_shape(cls, data: Any) -> Any:
        if not isinstance(data, dict):
            return data

        normalized = dict(data)
        order_number = normalized.get("order_number") or 1
        topic = normalized.get("topic") or normalized.get("_plan_topic") or "RAG topic"
        content_blocks = _normalize_content_blocks(normalized.get("content_blocks"))
        first_block = content_blocks[0] if content_blocks else ""

        normalized["content_blocks"] = content_blocks
        normalized.setdefault("title", f"Lesson {order_number}: {topic}")
        normalized.setdefault("topic", topic)
        normalized.setdefault("summary", first_block or normalized["title"])

        level = normalized.get("_plan_level")
        exercises = []
        for exercise in normalized.get("exercises") or []:
            if isinstance(exercise, dict) and level and not exercise.get("level"):
                exercise = {**exercise, "level": level}
            exercises.append(exercise)
        normalized["exercises"] = exercises

        return normalized


class RagLearningPlanResponse(BaseModel):
    title: str
    description: str
    goal: str
    language: str
    level: str
    duration: str
    lessons: list[RagLearningPlanLesson] = Field(..., min_length=1)
    sources: list[RagSource] = Field(default_factory=list)

    @model_validator(mode="before")
    @classmethod
    def normalize_llm_shape(cls, data: Any) -> Any:
        if not isinstance(data, dict):
            return data

        normalized = dict(data)
        topic = normalized.get("topic") or "RAG topic"
        learning_goal = normalized.get("learning_goal") or normalized.get("goal") or topic
        language = normalized.get("target_language") or normalized.get("language") or "German"
        level = normalized.get("level") or "A2"
        duration_weeks = normalized.get("duration_weeks")

        normalized.setdefault("title", f"{topic} Learning Plan")
        normalized.setdefault(
            "description",
            f"A RAG-grounded learning plan for {learning_goal}.",
        )
        normalized.setdefault("goal", learning_goal)
        normalized.setdefault("language", language)
        normalized.setdefault("level", level)
        normalized.setdefault(
            "duration",
            f"{duration_weeks} weeks" if duration_weeks else "Generated plan",
        )

        lessons = []
        for lesson in normalized.get("lessons") or []:
            if isinstance(lesson, dict):
                lesson = {**lesson, "_plan_topic": topic, "_plan_level": level}
            lessons.append(lesson)
        normalized["lessons"] = lessons

        return normalized


def _normalize_content_blocks(content_blocks: Any) -> list[str]:
    if not content_blocks:
        return []

    normalized = []
    for block in content_blocks:
        if isinstance(block, str):
            text = block
        elif isinstance(block, dict):
            text = (
                block.get("text")
                or block.get("content")
                or block.get("summary")
                or block.get("title")
                or ""
            )
        else:
            text = str(block)

        if text.strip():
            normalized.append(text.strip())

    return normalized
