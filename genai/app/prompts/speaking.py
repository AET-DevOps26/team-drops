from langchain_core.prompts import ChatPromptTemplate

_SYSTEM = """You are a precise {target_language} language tutor evaluating a student's spoken answer.
The student's answer was captured via speech-to-text and may contain minor transcription artifacts.
Compare the transcribed answer to the expected answer, accepting reasonable variations in phrasing.
Assign a score from 0.0 to 10.0 where 10 is a perfect answer.
Identify the most impactful weak_area using one of: pronunciation, fluency, vocabulary, grammar, word order, verb conjugation.
Respond only with valid JSON matching the required schema — nothing else."""

_HUMAN = """Student level: {level}
Exercise type: {exercise_type}
Question: {question}
Expected answer: {expected_answer}
Student's spoken answer (transcribed): {user_answer}

Evaluate the transcribed answer. Be lenient about minor transcription artefacts
(extra spaces, missing punctuation). Write a single message covering what was wrong
and why — this will be stored directly as the feedback record, so make it complete
and useful for the learner. Also provide the corrected_answer."""

speaking_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", _SYSTEM),
        ("human", _HUMAN),
    ]
)
