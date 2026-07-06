from app.prompts.corrections import corrections_prompt
from app.prompts.exercises import exercises_prompt
from app.prompts.listening import listening_questions_prompt, listening_script_prompt
from app.prompts.practice import practice_prompt
from app.prompts.rag import rag_learning_plan_prompt, rag_prompt
from app.prompts.speaking import speaking_prompt
from app.prompts.writing import writing_prompt


def _prompt_text(prompt) -> str:
    return "\n".join(str(message.prompt.template) for message in prompt.messages)


def test_prompt_input_variables_match_router_payloads():
    assert set(writing_prompt.input_variables) == {
        "exercise_type",
        "expected_answer",
        "level",
        "question",
        "target_language",
        "user_answer",
    }
    assert set(speaking_prompt.input_variables) == {
        "exercise_type",
        "expected_answer",
        "level",
        "question",
        "target_language",
        "user_answer",
    }
    assert set(exercises_prompt.input_variables) == {
        "count",
        "existing_exercises_json",
        "lesson_topic",
        "level",
        "target_language",
    }
    assert set(listening_script_prompt.input_variables) == {
        "level",
        "target_language",
        "topic",
    }
    assert set(listening_questions_prompt.input_variables) == {
        "count",
        "level",
        "script",
        "target_language",
    }
    assert set(practice_prompt.input_variables) == {
        "history",
        "level",
        "scenario",
        "target_language",
        "transcription",
    }
    assert set(corrections_prompt.input_variables) == {
        "history_json",
        "level",
        "scenario",
        "target_language",
    }
    assert set(rag_prompt.input_variables) == {"context", "question", "topic"}
    assert set(rag_learning_plan_prompt.input_variables) == {
        "context",
        "duration_weeks",
        "exercise_types",
        "learning_goal",
        "level",
        "maximum_lessons",
        "minimum_lessons",
        "study_hours_per_week",
        "target_language",
        "topic",
    }


def test_evaluation_prompts_include_rubrics_and_target_language_feedback():
    writing_text = _prompt_text(writing_prompt)
    speaking_text = _prompt_text(speaking_prompt)

    assert "Writing scoring rubric, total 10 points" in writing_text
    assert "Relevance to the question: 0-2 points" in writing_text
    assert "Professional or task-appropriate style: 0-1 point" in writing_text
    assert "Write learner-facing feedback in {target_language}" in writing_text

    assert "Speaking scoring rubric, total 10 points" in speaking_text
    assert "Task completion and meaning: 0-3 points" in speaking_text
    assert "Pronunciation/transcription tolerance: 0-2 points" in speaking_text
    assert "Write corrected learner answers in {target_language}" in speaking_text


def test_generation_prompts_include_quality_rules():
    exercise_text = _prompt_text(exercises_prompt)
    script_text = _prompt_text(listening_script_prompt)
    questions_text = _prompt_text(listening_questions_prompt)

    assert "Generate exactly {count} exercises" in exercise_text
    assert "Each expected_answer must be objectively checkable" in exercise_text
    assert "Do not duplicate the reference exercises" in exercise_text

    assert "Listening script quality rules" in script_text
    assert "Do not require outside knowledge" in script_text
    assert '"script"' in script_text

    assert "Listening question quality rules" in questions_text
    assert "Exactly one option must have is_correct=true" in questions_text
    assert 'a short sentence in {target_language}' in questions_text


def test_conversation_correction_and_rag_prompts_include_guardrails():
    practice_text = _prompt_text(practice_prompt)
    corrections_text = _prompt_text(corrections_prompt)
    rag_text = _prompt_text(rag_prompt)
    rag_learning_plan_text = _prompt_text(rag_learning_plan_prompt)

    assert "Keep the conversation moving with one natural follow-up question" in practice_text
    assert "Do not correct grammar or vocabulary during the conversation turn" in practice_text

    assert "Analyse only user turns" in corrections_text
    assert "return an empty corrections list" in corrections_text

    assert "Use only the retrieved context" in rag_text
    assert "Do not invent facts, citations, filenames, or page numbers" in rag_text
    assert "Use only the retrieved context" in rag_learning_plan_text
    assert "exactly between {minimum_lessons} and {maximum_lessons} lessons" in rag_learning_plan_text
    assert "reading, writing, listening, speaking" in rag_learning_plan_text
    assert "Do not invent unsupported enum spellings or hyphenated values" in rag_learning_plan_text
