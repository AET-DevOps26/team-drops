# ruff: noqa: E402
import asyncio
from pathlib import Path
import sys

from fastapi import APIRouter, HTTPException

for parent in Path(__file__).resolve().parents:
    if (parent / "RAG").is_dir() and str(parent) not in sys.path:
        sys.path.insert(0, str(parent))
        break

from RAG import build_corpus, list_topics, query_topic
from app.config import settings
from app.llm import get_llm, get_structured_llm
from app.prompts.rag import rag_learning_plan_prompt, rag_prompt
from app.schemas.rag import (
    GENERATED_PLAN_PLACEHOLDER,
    RAG_TOPIC_PLACEHOLDER,
    RagCorpusResponse,
    RagLearningPlanExercise,
    RagLearningPlanRequest,
    RagLearningPlanResponse,
    RagQueryRequest,
    RagQueryResponse,
    RagSource,
    RagTopicsResponse,
)  # noqa: E402

router = APIRouter(prefix="/rag", tags=["rag"])


@router.get(
    "/topics",
    operation_id="listRagTopics",
    response_model=RagTopicsResponse,
    summary="List available RAG topics",
    openapi_extra={"x-service": "genai-service"},
)
async def list_rag_topics() -> RagTopicsResponse:
    return RagTopicsResponse(topics=list_topics(_rag_doc_db()))


@router.post(
    "/topics/{topic}/corpus",
    operation_id="buildRagCorpus",
    response_model=RagCorpusResponse,
    summary="Build or rebuild a RAG topic corpus",
    description=(
        "Reads PDFs from `RAG doc DB/{topic}` and writes corpus files under "
        "`RAG doc DB/{topic}/corpus`."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def build_rag_corpus(topic: str) -> RagCorpusResponse:
    try:
        stats = await asyncio.to_thread(build_corpus, _rag_doc_db(), topic)
    except (FileNotFoundError, ValueError) as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=500, detail=f"Failed to build RAG corpus: {exc}"
        ) from exc

    return RagCorpusResponse(
        topic=stats.topic,
        pdf_count=stats.pdf_count,
        chunk_count=stats.chunk_count,
        corpus_dir=str(stats.corpus_dir),
    )


@router.post(
    "/query",
    operation_id="queryRag",
    response_model=RagQueryResponse,
    summary="Answer a question using a RAG topic",
    description=(
        "Retrieves context from PDFs under `RAG doc DB/{topic}`. If the corpus is "
        "missing or stale, it is rebuilt automatically under that topic's `corpus` folder."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def query_rag(body: RagQueryRequest) -> RagQueryResponse:
    chunks, context = await _retrieve_context(
        body.topic,
        body.question,
        top_k=body.top_k,
        rebuild=body.rebuild_corpus,
    )
    llm = get_llm()
    chain = rag_prompt | llm

    try:
        result = await chain.ainvoke(
            {"topic": body.topic, "question": body.question, "context": context}
        )
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"LLM invocation failed: {exc}"
        ) from exc

    return RagQueryResponse(
        topic=body.topic,
        question=body.question,
        answer=str(getattr(result, "content", result)),
        sources=[
            _source_from_chunk(chunk) for chunk in chunks
        ],
    )


@router.post(
    "/learning-plan",
    operation_id="generateRagLearningPlan",
    response_model=RagLearningPlanResponse,
    summary="Generate a structured learning plan from a RAG topic",
    description=(
        "Retrieves topic context from the local RAG corpus and asks the configured LLM "
        "for a schema-validated learning plan that learning-service can persist."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def generate_rag_learning_plan(
    body: RagLearningPlanRequest,
) -> RagLearningPlanResponse:
    if body.minimum_lessons > body.maximum_lessons:
        raise HTTPException(
            status_code=422,
            detail="minimum_lessons must be less than or equal to maximum_lessons",
        )

    chunks, context = await _retrieve_context(
        body.topic,
        body.learning_goal,
        top_k=body.top_k,
        rebuild=body.rebuild_corpus,
    )
    chain = rag_learning_plan_prompt | get_structured_llm(RagLearningPlanResponse)

    try:
        result: RagLearningPlanResponse = await asyncio.wait_for(
            chain.ainvoke(
                {
                    "topic": body.topic,
                    "learning_goal": body.learning_goal,
                    "target_language": body.target_language,
                    "level": body.level,
                    "duration_weeks": body.duration_weeks,
                    "study_hours_per_week": body.study_hours_per_week,
                    "minimum_lessons": body.minimum_lessons,
                    "maximum_lessons": body.maximum_lessons,
                    "exercise_types": ", ".join(body.exercise_types),
                    "context": context,
                }
            ),
            timeout=settings.llm_request_timeout_seconds,
        )
    except asyncio.TimeoutError as exc:
        raise HTTPException(
            status_code=504,
            detail=(
                "LLM learning-plan generation timed out after "
                f"{settings.llm_request_timeout_seconds} seconds"
            ),
        ) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"LLM learning-plan generation failed: {exc}"
        ) from exc

    lesson_count = len(result.lessons)
    if lesson_count < body.minimum_lessons or lesson_count > body.maximum_lessons:
        raise HTTPException(
            status_code=502,
            detail=(
                "LLM returned lesson count outside requested range: "
                f"{lesson_count} not in {body.minimum_lessons}-{body.maximum_lessons}"
            ),
        )

    return _complete_learning_plan_defaults(
        result,
        body,
        sources=[_source_from_chunk(chunk) for chunk in chunks],
    )


def _rag_doc_db() -> Path:
    return Path(settings.rag_doc_db_path)


async def _retrieve_context(
    topic: str,
    query: str,
    *,
    top_k: int,
    rebuild: bool,
):
    try:
        chunks = await asyncio.to_thread(
            query_topic,
            _rag_doc_db(),
            topic,
            query,
            top_k=top_k,
            rebuild=rebuild,
        )
    except (FileNotFoundError, ValueError) as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"RAG retrieval failed: {exc}") from exc

    return chunks, _format_context(chunks)


def _source_from_chunk(chunk) -> RagSource:
    return RagSource(
        source=chunk.source,
        page=chunk.page,
        chunk_index=chunk.chunk_index,
        score=chunk.score,
        text=chunk.text,
    )


def _complete_learning_plan_defaults(
    result: RagLearningPlanResponse,
    body: RagLearningPlanRequest,
    *,
    sources: list[RagSource],
) -> RagLearningPlanResponse:
    placeholder_title = f"{RAG_TOPIC_PLACEHOLDER} Learning Plan"
    placeholder_description = f"A RAG-grounded learning plan for {RAG_TOPIC_PLACEHOLDER}."
    placeholder_lesson_topic_prefix = f"{RAG_TOPIC_PLACEHOLDER} - "

    title = (
        f"{body.topic} Learning Plan"
        if result.title == placeholder_title
        else result.title
    )
    description = (
        f"A RAG-grounded learning plan for {body.learning_goal}."
        if result.description == placeholder_description
        else result.description
    )
    goal = body.learning_goal if result.goal == RAG_TOPIC_PLACEHOLDER else result.goal
    duration = (
        f"{body.duration_weeks} weeks"
        if result.duration == GENERATED_PLAN_PLACEHOLDER
        else result.duration
    )

    lessons = []
    for lesson in result.lessons:
        lesson_title = lesson.title
        if lesson_title == f"Lesson {lesson.order_number}: {RAG_TOPIC_PLACEHOLDER}":
            lesson_title = f"Lesson {lesson.order_number}: {body.topic}"

        lesson_topic = lesson.topic
        if lesson_topic == RAG_TOPIC_PLACEHOLDER:
            lesson_topic = body.topic
        elif lesson_topic.startswith(placeholder_lesson_topic_prefix):
            lesson_topic = f"{body.topic}{lesson_topic.removeprefix(RAG_TOPIC_PLACEHOLDER)}"

        lessons.append(
            lesson.model_copy(update={"title": lesson_title, "topic": lesson_topic})
        )

    lessons = _filter_requested_exercise_types(lessons, body)
    lessons = _ensure_requested_exercise_types(lessons, body)

    return result.model_copy(
        update={
            "title": title,
            "description": description,
            "goal": goal,
            "language": body.target_language,
            "level": body.level,
            "duration": duration,
            "lessons": lessons,
            "sources": sources,
        }
    )


def _ensure_requested_exercise_types(
    lessons,
    body: RagLearningPlanRequest,
):
    generated_types = {
        _type_for_exercise(exercise)
        for lesson in lessons
        for exercise in lesson.exercises
    }
    missing_types = [
        exercise_type
        for exercise_type in body.exercise_types
        if exercise_type not in generated_types
    ]
    if not missing_types or not lessons:
        return lessons

    first_lesson = lessons[0]
    fallback_exercises = [
        _fallback_exercise(exercise_type, body)
        for exercise_type in missing_types
    ]
    return [
        first_lesson.model_copy(
            update={"exercises": [*first_lesson.exercises, *fallback_exercises]}
        ),
        *lessons[1:],
    ]


def _filter_requested_exercise_types(
    lessons,
    body: RagLearningPlanRequest,
):
    requested_types = set(body.exercise_types)
    fallback_type = body.exercise_types[0] if body.exercise_types else "writing"
    filtered_lessons = []
    for lesson in lessons:
        requested_exercises = [
            exercise
            for exercise in lesson.exercises
            if _type_for_exercise(exercise) in requested_types
        ]
        if not requested_exercises:
            requested_exercises = [_fallback_exercise(fallback_type, body)]
        filtered_lessons.append(
            lesson.model_copy(update={"exercises": requested_exercises})
        )
    return filtered_lessons


def _type_for_exercise(exercise: RagLearningPlanExercise) -> str:
    match exercise.subtype:
        case "multiple_choice":
            return "reading"
        case "listening_choice":
            return "listening"
        case "speaking_prompt":
            return "speaking"
        case _:
            return "writing"


def _fallback_exercise(
    exercise_type: str,
    body: RagLearningPlanRequest,
) -> RagLearningPlanExercise:
    topic = body.topic
    goal = body.learning_goal
    language = body.target_language
    level = body.level

    match exercise_type:
        case "reading":
            return RagLearningPlanExercise(
                type="reading",
                subtype="multiple_choice",
                question=(
                    f"Which action best supports this learning goal about {topic}: {goal}?\n"
                    "A) Give a concise answer with a concrete example from the lesson context.\n"
                    "B) Ignore the topic and answer with unrelated personal details.\n"
                    "C) Focus only on memorized grammar rules without answering the question.\n"
                    "D) Change the subject instead of responding to the task."
                ),
                expected_answer=(
                    "A) Give a concise answer with a concrete example from the lesson context."
                ),
                difficulty=level,
            )
        case "listening":
            return RagLearningPlanExercise(
                type="listening",
                subtype="listening_choice",
                question=(
                    f"Generate a listening passage in {language} about {topic} "
                    f"for this learning goal: {goal}."
                ),
                expected_answer="Select the option that best reflects the speaker's main point.",
                difficulty=level,
            )
        case "speaking":
            return RagLearningPlanExercise(
                type="speaking",
                subtype="speaking_prompt",
                question=(
                    f"Answer in {language}: describe a concrete situation related to "
                    f"{topic}, what you did, and what the result was."
                ),
                expected_answer="A clear spoken answer with context, action, and result.",
                difficulty=level,
            )
        case _:
            return RagLearningPlanExercise(
                type="writing",
                subtype="free_text",
                question=(
                    f"Write a short {language} answer for this learning goal: {goal}."
                ),
                expected_answer="A structured written answer grounded in the lesson context.",
                difficulty=level,
            )


def _format_context(chunks) -> str:
    if not chunks:
        return "No relevant context was retrieved."
    return "\n\n".join(
        f"[{chunk.source}, page {chunk.page}, chunk {chunk.chunk_index}]\n{chunk.text}"
        for chunk in chunks
    )
