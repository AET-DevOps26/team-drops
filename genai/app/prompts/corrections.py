from langchain_core.prompts import ChatPromptTemplate

from app.prompts.rubrics import JSON_OUTPUT_RULES, SESSION_CORRECTION_RULES

_SYSTEM = """You are a {target_language} language teacher reviewing a learner's spoken practice session.
Analyse only the 'user' turns from the conversation below.
List meaningful grammar, vocabulary, or phrasing errors — ignore minor transcription artefacts
(extra spaces, missing punctuation, one-letter slips that are clearly transcription noise).
If the learner made no meaningful errors, return an empty corrections list.
{session_correction_rules}
{json_output_rules}"""

_HUMAN = """Learner level: {level}
Scenario: {scenario}

Full conversation (JSON):
{history_json}

Identify errors from the user turns only."""

corrections_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            _SYSTEM.format(
                target_language="{target_language}",
                session_correction_rules=SESSION_CORRECTION_RULES,
                json_output_rules=JSON_OUTPUT_RULES,
            ),
        ),
        ("human", _HUMAN),
    ]
)
