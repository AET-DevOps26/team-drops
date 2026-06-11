from langchain_core.prompts import ChatPromptTemplate

from app.prompts.rubrics import (
    JSON_OUTPUT_RULES,
    TARGET_LANGUAGE_FEEDBACK_RULES,
    WRITING_EVALUATION_RUBRIC,
)

_SYSTEM = """You are a precise {target_language} language tutor evaluating a student's written answer.
Compare the student's answer to the expected answer, accepting reasonable variations in phrasing.
Evaluate relevance, completeness, grammar and spelling, vocabulary usage, clarity and coherence, and professional communication style.
Identify the most impactful weak_area as a short grammatical or lexical phrase
(e.g. 'verb conjugation', 'word order', 'accents and diacritics').
{target_language_feedback_rules}
{writing_evaluation_rubric}
{json_output_rules}"""

_HUMAN = """Student level: {level}
Exercise type: {exercise_type}
Question: {question}
Expected answer: {expected_answer}
Student's answer: {user_answer}

Evaluate the answer using the rubric. Write a single message that covers what was correct,
what was wrong, and why. The message will be stored directly as the feedback record, so make it
complete, useful, and written in {target_language}. Also provide corrected_answer."""

writing_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            _SYSTEM.format(
                target_language="{target_language}",
                target_language_feedback_rules=TARGET_LANGUAGE_FEEDBACK_RULES,
                writing_evaluation_rubric=WRITING_EVALUATION_RUBRIC,
                json_output_rules=JSON_OUTPUT_RULES,
            ),
        ),
        ("human", _HUMAN),
    ]
)
