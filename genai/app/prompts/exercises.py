from langchain_core.prompts import ChatPromptTemplate

from app.prompts.rubrics import EXERCISE_GENERATION_RULES, JSON_OUTPUT_RULES

_SYSTEM = """You are an expert language learning content designer.
Your task is to generate new practice exercises for a language learning app.
You MUST follow the exact difficulty level and exercise style of the provided reference examples.
{exercise_generation_rules}
{json_output_rules}"""

_HUMAN = """Generate {count} new {target_language} exercises for a {level} learner.
Lesson topic: {lesson_topic}

Reference exercises (match their style, format, and difficulty exactly):
{existing_exercises_json}

Requirements:
- All exercises must target {level} proficiency in {target_language}
- Match the exercise types found in the reference set
- Each exercise must include a clear expected_answer
- Vary the question formats slightly to avoid repetition
- Keep questions concise and unambiguous
- Return exactly {count} exercises"""

exercises_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            _SYSTEM.format(
                exercise_generation_rules=EXERCISE_GENERATION_RULES,
                json_output_rules=JSON_OUTPUT_RULES,
            ),
        ),
        ("human", _HUMAN),
    ]
)
