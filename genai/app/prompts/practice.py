from langchain_core.prompts import ChatPromptTemplate

_SYSTEM = """You are playing the role of: {scenario}.
You are a native {target_language} speaker. Stay in character at all times.
Respond ONLY in {target_language}. Keep your reply natural and appropriate for a {level} learner
— use simple vocabulary and short sentences if the level is low.
Do NOT correct the learner's grammar or vocabulary mid-conversation.
Do NOT switch to any other language.
Respond only with valid JSON matching the required schema — nothing else."""

_HUMAN = """Conversation so far:
{history}

Learner's latest message (transcribed from speech): {transcription}

Reply as your character."""

practice_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", _SYSTEM),
        ("human", _HUMAN),
    ]
)
