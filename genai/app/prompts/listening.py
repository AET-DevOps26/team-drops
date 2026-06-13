from langchain_core.prompts import ChatPromptTemplate

from app.prompts.rubrics import (
    JSON_OUTPUT_RULES,
    LISTENING_QUESTION_RULES,
    LISTENING_SCRIPT_RULES,
)

_SCRIPT_SYSTEM = """You are a {target_language} language teacher creating listening comprehension material.
Write a short, natural-sounding passage at the specified CEFR level.
The passage should be suitable for reading aloud and contain enough detail for comprehension questions.
Use vocabulary and grammar structures appropriate for the given level — avoid anything above it.
{listening_script_rules}
{json_output_rules}"""

_SCRIPT_HUMAN = """Language: {target_language}
CEFR level: {level}
Topic hint: {topic}

Write a listening passage of 80–120 words in {target_language}.
The passage must be self-contained so a listener can answer questions about it without outside knowledge."""

listening_script_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            _SCRIPT_SYSTEM.format(
                target_language="{target_language}",
                listening_script_rules=LISTENING_SCRIPT_RULES,
                json_output_rules=JSON_OUTPUT_RULES,
            ),
        ),
        ("human", _SCRIPT_HUMAN),
    ]
)

_QUESTIONS_SYSTEM = """You are a {target_language} language teacher writing multiple-choice comprehension questions.
Generate exactly {count} questions based solely on the content of the provided listening script.
Each question must have exactly four options — exactly one must be correct and the other three plausible but wrong.
Do not invent information not present in the script.
{listening_question_rules}
{json_output_rules}"""

_QUESTIONS_HUMAN = """Language: {target_language}
CEFR level: {level}

Script:
{script}

Write {count} multiple-choice question(s). For each question provide:
- "question": the question text in {target_language}
- "options": a list of exactly 4 objects, each with "text" (string) and "is_correct" (bool); exactly one true
- "explanation": a short sentence in {target_language} explaining why the correct answer is right"""

listening_questions_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            _QUESTIONS_SYSTEM.format(
                count="{count}",
                target_language="{target_language}",
                listening_question_rules=LISTENING_QUESTION_RULES,
                json_output_rules=JSON_OUTPUT_RULES,
            ),
        ),
        ("human", _QUESTIONS_HUMAN),
    ]
)
