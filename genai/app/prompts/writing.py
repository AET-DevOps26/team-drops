from langchain_core.prompts import ChatPromptTemplate

from app.prompts.rubrics import (
    JSON_OUTPUT_RULES,
    TARGET_LANGUAGE_FEEDBACK_RULES,
    WRITING_EVALUATION_RUBRIC,
)

_SYSTEM = """You are a precise {target_language} language tutor evaluating a student's written answer.
Compare the student's answer to the expected answer, accepting reasonable variations in phrasing.
The student's answer is expected to be in {target_language}. Do not penalize a correct
{target_language} answer because the question/source text is written in another language.
Evaluate relevance, completeness, grammar and spelling, vocabulary usage, clarity and coherence, and professional communication style.
Identify the most impactful weak_area as a short grammatical or lexical phrase
(e.g. 'verb conjugation', 'word order', 'accents and diacritics').
corrected_answer must be the corrected version of the student's answer in {target_language},
not an English translation or explanation.
Keep message and corrected_answer each shorter than 255 characters.
{target_language_feedback_rules}
{writing_evaluation_rubric}
{json_output_rules}"""

_HUMAN = """Student level: {level}
Exercise type: {exercise_type}
Question: {question}
Expected answer: {expected_answer}
Student's answer: {user_answer}

Evaluate the answer using the rubric. Write one concise feedback message under 255 characters
that covers the main issue and why. Also provide corrected_answer under 255 characters in
{target_language}."""

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
