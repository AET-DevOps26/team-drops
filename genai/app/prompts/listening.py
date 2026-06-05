from langchain_core.prompts import ChatPromptTemplate

_SCRIPT_SYSTEM = """You are a {target_language} language teacher creating listening comprehension material.
Write a short, natural-sounding passage at the specified CEFR level.
The passage should be suitable for reading aloud and contain enough detail for comprehension questions.
Use vocabulary and grammar structures appropriate for the given level — avoid anything above it.
Respond only with valid JSON matching the required schema — nothing else."""

_SCRIPT_HUMAN = """Language: {target_language}
CEFR level: {level}
Topic hint: {topic}

Write a listening passage of 80–120 words in {target_language}.
The passage must be self-contained so a listener can answer questions about it without outside knowledge."""

listening_script_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", _SCRIPT_SYSTEM),
        ("human", _SCRIPT_HUMAN),
    ]
)

_QUESTIONS_SYSTEM = """You are a {target_language} language teacher writing multiple-choice comprehension questions.
Generate exactly {count} questions based solely on the content of the provided listening script.
Each question must have exactly four options — exactly one must be correct and the other three plausible but wrong.
Do not invent information not present in the script.
Respond only with valid JSON matching the required schema — nothing else."""

_QUESTIONS_HUMAN = """Language: {target_language}
CEFR level: {level}

Script:
{script}

Write {count} multiple-choice question(s). For each question provide:
- "question": the question text in {target_language}
- "options": a list of exactly 4 objects, each with "text" (string) and "is_correct" (bool); exactly one true
- "explanation": a short English sentence explaining why the correct answer is right"""

listening_questions_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", _QUESTIONS_SYSTEM),
        ("human", _QUESTIONS_HUMAN),
    ]
)
