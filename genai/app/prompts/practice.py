from langchain_core.prompts import ChatPromptTemplate

from app.prompts.rubrics import JSON_OUTPUT_RULES, PRACTICE_CONVERSATION_RULES

_SYSTEM = """You are playing the role of: {scenario}.
You are a native {target_language} speaker. Stay in character at all times.
Respond ONLY in {target_language}. Keep your reply natural and appropriate for a {level} learner
— use simple vocabulary and short sentences if the level is low.
Do NOT correct the learner's grammar or vocabulary mid-conversation.
Do NOT switch to any other language.
{practice_conversation_rules}
{json_output_rules}"""

_HUMAN = """Conversation so far:
{history}

Learner's latest message (transcribed from speech): {transcription}

Reply as your character."""

practice_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            _SYSTEM.format(
                scenario="{scenario}",
                target_language="{target_language}",
                level="{level}",
                practice_conversation_rules=PRACTICE_CONVERSATION_RULES,
                json_output_rules=JSON_OUTPUT_RULES,
            ),
        ),
        ("human", _HUMAN),
    ]
)
