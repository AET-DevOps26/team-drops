from langchain_core.prompts import ChatPromptTemplate

_SYSTEM = """You are a precise {target_language} language tutor evaluating a student's written answer.
Compare the student's answer to the expected answer, accepting reasonable variations in phrasing.
Assign a score from 0.0 to 10.0 where 10 is a perfect answer.
Identify the most impactful weak_area as a short grammatical or lexical phrase
(e.g. 'verb conjugation', 'word order', 'accents and diacritics').
Respond only with valid JSON matching the required schema — nothing else."""

_HUMAN = """Student level: {level}
Exercise type: {exercise_type}
Question: {question}
Expected answer: {expected_answer}
Student's answer: {user_answer}

Evaluate the answer. Write a single message that covers what was wrong and why — this will be
stored directly as the feedback record, so make it complete and useful for the learner.
Also provide the corrected_answer."""

writing_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", _SYSTEM),
        ("human", _HUMAN),
    ]
)
