import json

from fastapi import APIRouter, HTTPException

from app.llm import get_llm
from app.prompts.exercises import exercises_prompt
from app.schemas.exercises import GenerateExercisesRequest, GenerateExercisesResponse

router = APIRouter(prefix="/exercises", tags=["exercises"])


@router.post(
    "/generate",
    response_model=GenerateExercisesResponse,
    summary="Generate additional exercises",
    description=(
        "Given a lesson topic and a set of existing exercises as style references, "
        "generate new exercises using AI. The AI matches the format, difficulty, and "
        "exercise types of the provided reference set."
    ),
)
async def generate_exercises(body: GenerateExercisesRequest) -> GenerateExercisesResponse:
    llm = get_llm()
    chain = exercises_prompt | llm.with_structured_output(GenerateExercisesResponse)

    existing_json = json.dumps(
        [ex.model_dump() for ex in body.existing_exercises],
        ensure_ascii=False,
        indent=2,
    )

    try:
        result: GenerateExercisesResponse = await chain.ainvoke({
            "count": body.count,
            "target_language": body.target_language,
            "level": body.level,
            "lesson_topic": body.lesson_topic,
            "existing_exercises_json": existing_json,
        })
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"LLM invocation failed: {exc}") from exc

    for exercise in result.exercises:
        exercise.lesson_id = body.lesson_id

    return result
