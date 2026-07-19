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
    GENERATED_PLAN_PLACEHOLDER,
    RAG_TOPIC_PLACEHOLDER,
    RagCorpusResponse,
    RagLearningPlanExercise,
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
        result: RagLearningPlanResponse = await asyncio.wait_for(
            chain.ainvoke(generation_payload),
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

    _ensure_lesson_count(result, body)
    candidate = result
    review = await _review_learning_plan(candidate, body, context, quality_policy)
    if review.accepted:
        logger.info(
            "Accepted RAG learning plan topic=%s lessons=%s retry=false",
            body.topic,
            len(candidate.lessons),
        )
        return _complete_learning_plan_defaults(
            candidate,
            body,
            sources=[_source_from_chunk(chunk) for chunk in chunks],
        )

    max_attempts = settings.rag_learning_plan_max_repair_attempts
    for attempt in range(1, max_attempts + 1):
        logger.warning(
            "Rejected RAG learning plan topic=%s violations=%s; corrective_attempt=%s/%s",
            body.topic,
            _violation_log_values(review),
            attempt,
            max_attempts,
        )
        repair_payload = {
            **generation_payload,
            "plan_json": candidate.model_dump_json(),
            "violations_json": json.dumps(
                [violation.model_dump() for violation in review.violations],
                ensure_ascii=False,
            ),
        }
        repair_chain = rag_learning_plan_repair_prompt | get_structured_llm(
            RagLearningPlanResponse
        )
        try:
            candidate = await asyncio.wait_for(
                repair_chain.ainvoke(repair_payload),
                timeout=settings.llm_request_timeout_seconds,
            )
        except asyncio.TimeoutError as exc:
            raise HTTPException(
                status_code=504,
                detail=(
                    "LLM learning-plan corrective regeneration timed out after "
                    f"{settings.llm_request_timeout_seconds} seconds"
                ),
            ) from exc
        except Exception as exc:
            raise HTTPException(
                status_code=502,
                detail=f"LLM learning-plan corrective regeneration failed: {exc}",
            ) from exc

        _ensure_lesson_count(candidate, body)
        review = await _review_learning_plan(candidate, body, context, quality_policy)
        if review.accepted:
            logger.info(
                "Accepted corrected RAG learning plan topic=%s lessons=%s "
                "corrective_attempt=%s/%s",
                body.topic,
                len(candidate.lessons),
                attempt,
                max_attempts,
            )
            return _complete_learning_plan_defaults(
                candidate,
                body,
                sources=[_source_from_chunk(chunk) for chunk in chunks],
            )

    logger.warning(
        "Returning last RAG learning plan after quality review failed "
        "topic=%s attempts=%s violations=%s",
        body.topic,
        max_attempts,
        _violation_log_values(review),
    )
    return _complete_learning_plan_defaults(
        candidate,
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
    placeholder_title = f"{RAG_TOPIC_PLACEHOLDER} Learning Plan"
    placeholder_description = (
        f"A RAG-grounded learning plan for {RAG_TOPIC_PLACEHOLDER}."
    )
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
            lesson_topic = (
                f"{body.topic}{lesson_topic.removeprefix(RAG_TOPIC_PLACEHOLDER)}"
            )

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
        _fallback_exercise(exercise_type, body) for exercise_type in missing_types
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
