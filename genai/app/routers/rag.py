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
    RagCorpusResponse,
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
        result: RagLearningPlanResponse = await chain.ainvoke(
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
        )
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
    title = (
        f"{body.topic} Learning Plan"
        if result.title == "RAG topic Learning Plan"
        else result.title
    )
    description = (
        f"A RAG-grounded learning plan for {body.learning_goal}."
        if result.description == "A RAG-grounded learning plan for RAG topic."
        else result.description
    )
    goal = body.learning_goal if result.goal == "RAG topic" else result.goal
    duration = (
        f"{body.duration_weeks} weeks"
        if result.duration == "Generated plan"
        else result.duration
    )

    lessons = []
    for lesson in result.lessons:
        lesson_title = lesson.title
        if lesson_title == f"Lesson {lesson.order_number}: RAG topic":
            lesson_title = f"Lesson {lesson.order_number}: {body.topic}"

        lesson_topic = lesson.topic
        if lesson_topic == "RAG topic":
            lesson_topic = body.topic
        elif lesson_topic.startswith("RAG topic - "):
            lesson_topic = f"{body.topic}{lesson_topic.removeprefix('RAG topic')}"

        lessons.append(
            lesson.model_copy(update={"title": lesson_title, "topic": lesson_topic})
        )

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


def _format_context(chunks) -> str:
    if not chunks:
        return "No relevant context was retrieved."
    return "\n\n".join(
        f"[{chunk.source}, page {chunk.page}, chunk {chunk.chunk_index}]\n{chunk.text}"
        for chunk in chunks
    )
