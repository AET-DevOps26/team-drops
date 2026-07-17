# ruff: noqa: E402
import asyncio
import json
import logging
from pathlib import Path
import re
import sys

from fastapi import APIRouter, HTTPException

for parent in Path(__file__).resolve().parents:
    if (parent / "RAG").is_dir() and str(parent) not in sys.path:
        sys.path.insert(0, str(parent))
        break

from RAG import build_corpus, list_topics, query_topic
from app.config import settings
from app.llm import get_llm, get_structured_llm
from app.prompts.rag import (
    rag_learning_plan_prompt,
    rag_learning_plan_repair_prompt,
    rag_learning_plan_review_prompt,
    rag_prompt,
)
from app.schemas.rag import (
    RagCorpusResponse,
    RagLearningPlanRequest,
    RagLearningPlanQualityReview,
    RagLearningPlanResponse,
    RagPlanQualityViolation,
    RagQueryRequest,
    RagQueryResponse,
    RagSource,
    RagTopicsResponse,
)  # noqa: E402

router = APIRouter(prefix="/rag", tags=["rag"])
logger = logging.getLogger(__name__)

_JOB_INTERVIEW_POLICY = """
- Build the entire plan around practising realistic interview answers and interviewer interactions.
- Speaking exercises must ask a concrete interview question that the learner answers as the candidate,
  or require a realistic interview role-play, clarification, or follow-up response.
- Speaking expected answers must state the key content or answer structure the learner should include.
- Prefer behavioral and STAR answers, technical explanations, project discussions, motivation,
  strengths and weaknesses, problem solving, teamwork, conflict, and candidate follow-up questions.
- Exclude attire, clothing, grooming, appearance, travel, arrival time, equipment checks,
  preparation checklists, and other passive interview logistics from all lessons and exercises.
""".strip()

_GENERIC_QUALITY_POLICY = """
- Every lesson and exercise must directly advance the stated learning goal.
- Speaking exercises must require meaningful spoken language production or realistic interaction.
- Match the requested exercise type, target language, and CEFR level.
- Keep every question self-contained and give a concrete, useful expected answer.
""".strip()

_JOB_INTERVIEW_RETRIEVAL_FOCUS = (
    "behavioral interview questions STAR answers technical explanations software projects "
    "problem solving teamwork conflict strengths weaknesses motivation follow-up questions role-play"
)

_PASSIVE_INTERVIEW_PATTERNS = tuple(
    re.compile(pattern, re.IGNORECASE)
    for pattern in (
        r"\bwhat (?:you should|to) wear\b",
        r"\b(?:wear|attire|clothing|outfit|grooming|dress professionally)\b",
        r"\b(?:arrive|arrival) (?:early|on time)\b",
        r"\b(?:equipment|preparation) checklist\b",
        r"\b(?:was|sollte) (?:man|ich|du) (?:tragen|anziehen)\b",
        r"\b(?:kleidung|outfit|erscheinungsbild|körperpflege|gepflegt)\b",
        r"\b(?:früh|pünktlich) ankommen\b",
        r"\b(?:ausrüstungs|vorbereitungs)?checkliste\b",
        r"\b(?:quoi|que) porter\b",
        r"\b(?:tenue|vêtements|habillement|toilettage)\b",
        r"\barriver (?:tôt|à l'heure)\b",
        r"\b(?:liste de contrôle|équipement)\b",
    )
)


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
        sources=[_source_from_chunk(chunk) for chunk in chunks],
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

    retrieval_query = _learning_plan_retrieval_query(body)
    chunks, context = await _retrieve_context(
        body.topic,
        retrieval_query,
        top_k=body.top_k,
        rebuild=body.rebuild_corpus,
    )
    quality_policy = _quality_policy(body.topic)
    generation_payload = _generation_payload(body, context, quality_policy)
    chain = rag_learning_plan_prompt | get_structured_llm(RagLearningPlanResponse)

    try:
        result: RagLearningPlanResponse = await chain.ainvoke(generation_payload)
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"LLM learning-plan generation failed: {exc}"
        ) from exc

    _ensure_lesson_count(result, body)
    review = await _review_learning_plan(result, body, context, quality_policy)
    if review.accepted:
        logger.info(
            "Accepted RAG learning plan topic=%s lessons=%s retry=false",
            body.topic,
            len(result.lessons),
        )
        return _complete_learning_plan_defaults(
            result,
            body,
            sources=[_source_from_chunk(chunk) for chunk in chunks],
        )

    logger.warning(
        "Rejected RAG learning plan topic=%s violations=%s; retrying once",
        body.topic,
        _violation_log_values(review),
    )
    repair_payload = {
        **generation_payload,
        "plan_json": result.model_dump_json(),
        "violations_json": json.dumps(
            [violation.model_dump() for violation in review.violations],
            ensure_ascii=False,
        ),
    }
    repair_chain = rag_learning_plan_repair_prompt | get_structured_llm(
        RagLearningPlanResponse
    )
    try:
        repaired: RagLearningPlanResponse = await repair_chain.ainvoke(repair_payload)
    except Exception as exc:
        raise HTTPException(
            status_code=502,
            detail=f"LLM learning-plan corrective regeneration failed: {exc}",
        ) from exc

    _ensure_lesson_count(repaired, body)
    repaired_review = await _review_learning_plan(
        repaired, body, context, quality_policy
    )
    if not repaired_review.accepted:
        logger.error(
            "RAG learning-plan quality failed after retry topic=%s violations=%s",
            body.topic,
            _violation_log_values(repaired_review),
        )
        raise HTTPException(
            status_code=502,
            detail=(
                "Generated RAG learning plan did not meet exercise quality requirements "
                "after one corrective retry"
            ),
        )

    logger.info(
        "Accepted corrected RAG learning plan topic=%s lessons=%s retry=true",
        body.topic,
        len(repaired.lessons),
    )

    return _complete_learning_plan_defaults(
        repaired,
        body,
        sources=[_source_from_chunk(chunk) for chunk in chunks],
    )


def _learning_plan_retrieval_query(body: RagLearningPlanRequest) -> str:
    exercise_focus = " ".join(body.exercise_types)
    if _is_job_interview_topic(body.topic):
        return (
            f"{body.learning_goal} {exercise_focus} language practice "
            f"{_JOB_INTERVIEW_RETRIEVAL_FOCUS}"
        )
    return f"{body.learning_goal} {exercise_focus} language practice"


def _quality_policy(topic: str) -> str:
    return (
        _JOB_INTERVIEW_POLICY
        if _is_job_interview_topic(topic)
        else _GENERIC_QUALITY_POLICY
    )


def _is_job_interview_topic(topic: str) -> bool:
    return " ".join(topic.casefold().split()) == "job interview"


def _generation_payload(
    body: RagLearningPlanRequest, context: str, quality_policy: str
) -> dict[str, object]:
    return {
        "topic": body.topic,
        "learning_goal": body.learning_goal,
        "target_language": body.target_language,
        "level": body.level,
        "duration_weeks": body.duration_weeks,
        "study_hours_per_week": body.study_hours_per_week,
        "minimum_lessons": body.minimum_lessons,
        "maximum_lessons": body.maximum_lessons,
        "exercise_types": ", ".join(body.exercise_types),
        "quality_policy": quality_policy,
        "context": context,
    }


async def _review_learning_plan(
    result: RagLearningPlanResponse,
    body: RagLearningPlanRequest,
    context: str,
    quality_policy: str,
) -> RagLearningPlanQualityReview:
    review_chain = rag_learning_plan_review_prompt | get_structured_llm(
        RagLearningPlanQualityReview
    )
    try:
        review: RagLearningPlanQualityReview = await review_chain.ainvoke(
            {
                "topic": body.topic,
                "learning_goal": body.learning_goal,
                "target_language": body.target_language,
                "level": body.level,
                "exercise_types": ", ".join(body.exercise_types),
                "quality_policy": quality_policy,
                "context": context,
                "plan_json": result.model_dump_json(),
            }
        )
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"LLM learning-plan quality review failed: {exc}"
        ) from exc

    deterministic = _deterministic_quality_violations(result, body.topic)
    if deterministic:
        return review.model_copy(
            update={
                "accepted": False,
                "violations": [*review.violations, *deterministic],
            }
        )
    return review


def _deterministic_quality_violations(
    result: RagLearningPlanResponse, topic: str
) -> list[RagPlanQualityViolation]:
    if not _is_job_interview_topic(topic):
        return []

    violations: list[RagPlanQualityViolation] = []
    for lesson in result.lessons:
        lesson_text = " ".join(
            [lesson.title, lesson.topic, lesson.summary, *lesson.content_blocks]
        )
        if _contains_passive_interview_topic(lesson_text):
            violations.append(
                RagPlanQualityViolation(
                    lesson_order=lesson.order_number,
                    code="passive_interview_preparation",
                    reason=(
                        "Lesson focuses on attire, grooming, timing, equipment, or other "
                        "passive preparation instead of practising interview answers."
                    ),
                )
            )
        for exercise_index, exercise in enumerate(lesson.exercises):
            exercise_text = f"{exercise.question} {exercise.expected_answer}"
            if _contains_passive_interview_topic(exercise_text):
                violations.append(
                    RagPlanQualityViolation(
                        lesson_order=lesson.order_number,
                        exercise_index=exercise_index,
                        code="passive_interview_exercise",
                        reason=(
                            "Exercise asks about attire, grooming, timing, equipment, or "
                            "other passive preparation; replace it with interview-answer practice."
                        ),
                    )
                )
    return violations


def _contains_passive_interview_topic(value: str) -> bool:
    return any(pattern.search(value) for pattern in _PASSIVE_INTERVIEW_PATTERNS)


def _ensure_lesson_count(
    result: RagLearningPlanResponse, body: RagLearningPlanRequest
) -> None:
    lesson_count = len(result.lessons)
    if lesson_count < body.minimum_lessons or lesson_count > body.maximum_lessons:
        raise HTTPException(
            status_code=502,
            detail=(
                "LLM returned lesson count outside requested range: "
                f"{lesson_count} not in {body.minimum_lessons}-{body.maximum_lessons}"
            ),
        )


def _violation_log_values(review: RagLearningPlanQualityReview) -> list[str]:
    return [violation.code for violation in review.violations]


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
        raise HTTPException(
            status_code=500, detail=f"RAG retrieval failed: {exc}"
        ) from exc

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
