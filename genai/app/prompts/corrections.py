from langchain_core.prompts import ChatPromptTemplate

_SYSTEM = """You are a {target_language} language teacher reviewing a learner's spoken practice session.
Analyse only the 'user' turns from the conversation below.
List meaningful grammar, vocabulary, or phrasing errors — ignore minor transcription artefacts
(extra spaces, missing punctuation, one-letter slips that are clearly transcription noise).
If the learner made no meaningful errors, return an empty corrections list.
Respond only with valid JSON matching the required schema — nothing else."""

_HUMAN = """Learner level: {level}
Scenario: {scenario}

Full conversation (JSON):
{history_json}

Identify errors from the user turns only."""

corrections_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", _SYSTEM),
        ("human", _HUMAN),
    ]
)
