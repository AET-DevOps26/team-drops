from langchain_core.prompts import ChatPromptTemplate

_SYSTEM = """You are an expert language learning content designer.
Your task is to generate new practice exercises for a language learning app.
You MUST follow the exact difficulty level and exercise style of the provided reference examples.
Always respond with valid JSON matching the required schema — nothing else."""

_HUMAN = """Generate {count} new {target_language} exercises for a {level} learner.
Lesson topic: {lesson_topic}

Reference exercises (match their style, format, and difficulty exactly):
{existing_exercises_json}

Requirements:
- All exercises must target {level} proficiency in {target_language}
- Match the exercise types found in the reference set
- Each exercise must include a clear expected_answer
- Vary the question formats slightly to avoid repetition
- Keep questions concise and unambiguous"""

exercises_prompt = ChatPromptTemplate.from_messages([
    ("system", _SYSTEM),
    ("human", _HUMAN),
])
