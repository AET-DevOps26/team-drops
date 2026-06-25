from fastapi import APIRouter, HTTPException

from app.llm import get_structured_llm
from app.prompts.writing import writing_prompt
from app.schemas.writing import WritingEvaluationRequest, WritingEvaluationResponse

router = APIRouter(prefix="/writing", tags=["writing"])


@router.post(
    "/evaluate",
    operation_id="evaluateWriting",
    response_model=WritingEvaluationResponse,
    summary="Evaluate a written answer",
    description=(
        "Evaluate a learner's written answer against the exercise's expected answer. "
        "Returns a numeric score (0–10) ready to store as UserAnswer.score, "
        "plus message and weak_area fields that map directly to a Feedback record. "
        "Also returns the corrected answer and a detailed explanation of errors."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def evaluate_writing(body: WritingEvaluationRequest) -> WritingEvaluationResponse:
    chain = writing_prompt | get_structured_llm(WritingEvaluationResponse)

    try:
        result: WritingEvaluationResponse = await chain.ainvoke(
            {
                "target_language": body.target_language,
                "level": body.level,
                "exercise_type": body.exercise_type,
                "question": body.question,
                "expected_answer": body.expected_answer,
                "user_answer": body.user_answer,
            }
        )
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"LLM invocation failed: {exc}"
        ) from exc

    return result
