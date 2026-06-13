from langchain_core.prompts import ChatPromptTemplate

from app.prompts.rubrics import (
    JSON_OUTPUT_RULES,
    SPEAKING_EVALUATION_RUBRIC,
    TARGET_LANGUAGE_FEEDBACK_RULES,
)

_SYSTEM = """You are a precise {target_language} language tutor evaluating a student's spoken answer.
The student's answer was captured via speech-to-text and may contain minor transcription artifacts.
Compare the transcribed answer to the expected answer, accepting reasonable variations in phrasing.
Evaluate task completion, grammar, vocabulary, fluency/naturalness, and pronunciation/transcription tolerance.
Identify the most impactful weak_area using one of: pronunciation, fluency, vocabulary, grammar, word order, verb conjugation.
{target_language_feedback_rules}
{speaking_evaluation_rubric}
{json_output_rules}"""

_HUMAN = """Student level: {level}
Exercise type: {exercise_type}
Question: {question}
Expected answer: {expected_answer}
Student's spoken answer (transcribed): {user_answer}

Evaluate the transcribed answer using the rubric. Be lenient about minor transcription artifacts
(extra spaces, missing punctuation, obvious one-word misrecognitions when the intended meaning is clear).
Write a single message covering what was correct, what was wrong, and why. The message will be
stored directly as the feedback record, so make it complete, useful, and written in {target_language}.
Also provide corrected_answer."""

speaking_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            _SYSTEM.format(
                target_language="{target_language}",
                target_language_feedback_rules=TARGET_LANGUAGE_FEEDBACK_RULES,
                speaking_evaluation_rubric=SPEAKING_EVALUATION_RUBRIC,
                json_output_rules=JSON_OUTPUT_RULES,
            ),
        ),
        ("human", _HUMAN),
    ]
)
