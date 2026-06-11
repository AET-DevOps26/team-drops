JSON_OUTPUT_RULES = """
Output rules:
- Return only the fields required by the structured output schema.
- Do not include markdown, comments, extra keys, or text outside the schema.
- Use finite numeric scores within the schema range.
- If an input is empty, unrelated, unsafe to evaluate, or impossible to understand, still return a valid schema response with the lowest appropriate score.
"""

TARGET_LANGUAGE_FEEDBACK_RULES = """
Language rules:
- Write learner-facing feedback in {target_language}.
- Write corrected learner answers in {target_language}.
- Do not switch to English unless {target_language} is English.
- Keep feedback encouraging, specific, and concise enough to be shown directly in the app.
"""

WRITING_EVALUATION_RUBRIC = """
Writing scoring rubric, total 10 points:
- Relevance to the question: 0-2 points. 2 fully answers the prompt; 1 partially addresses it; 0 is unrelated or in the wrong language.
- Completeness: 0-2 points. 2 includes all required information; 1 misses some required detail; 0 is empty or too short to evaluate.
- Grammar and spelling: 0-2 points. 2 has only negligible errors; 1 has noticeable but understandable errors; 0 has frequent errors that block meaning.
- Vocabulary usage: 0-1.5 points. 1.5 uses accurate, level-appropriate words; 0.75 has some wrong or awkward word choices; 0 uses mostly unsuitable vocabulary.
- Clarity and coherence: 0-1.5 points. 1.5 is easy to follow and well organized; 0.75 is understandable but choppy; 0 is unclear.
- Professional or task-appropriate style: 0-1 point. 1 matches the expected tone/register; 0.5 is somewhat informal or unnatural; 0 is inappropriate for the task.

Score bands:
- 10: Fully correct, complete, natural, and task-appropriate.
- 8-9: Meaning is correct with only minor language or style issues.
- 6-7: Mostly understandable and relevant, but with several errors or omissions.
- 4-5: Partially relevant, incomplete, or error-heavy.
- 1-3: Barely addresses the task or is very difficult to understand.
- 0: Empty, unrelated, wrong target language, or impossible to evaluate.

Set is_correct to true only when the answer is substantially correct for the exercise, even if it has small language mistakes.
"""

SPEAKING_EVALUATION_RUBRIC = """
Speaking scoring rubric, total 10 points:
- Task completion and meaning: 0-3 points. 3 fully answers the prompt; 1-2 partially answers it; 0 is unrelated or wrong language.
- Grammar: 0-2 points. 2 has only minor errors; 1 has repeated errors but meaning survives; 0 has errors that block meaning.
- Vocabulary: 0-1.5 points. 1.5 is accurate and level-appropriate; 0.75 is limited or partly wrong; 0 is mostly unsuitable.
- Fluency and naturalness: 0-1.5 points. 1.5 sounds natural for the level; 0.75 is understandable but awkward; 0 is fragmented.
- Pronunciation/transcription tolerance: 0-2 points. 2 is clear or only has minor STT artifacts; 1 has possible pronunciation or transcription issues but meaning is recoverable; 0 is not understandable.

Score bands:
- 10: Fully correct, complete, natural, and clear.
- 8-9: Correct meaning with only minor spoken-language issues.
- 6-7: Mostly understandable and relevant, but with several errors.
- 4-5: Partially relevant or difficult to follow.
- 1-3: Barely addresses the task or is mostly unintelligible.
- 0: Empty transcription, unrelated, wrong target language, or impossible to evaluate.

Set is_correct to true only when the spoken answer is substantially correct for the exercise. Be lenient about obvious speech-to-text artifacts, but do not ignore real grammar, vocabulary, or meaning errors.
"""

EXERCISE_GENERATION_RULES = """
Exercise generation quality rules:
- Generate exactly {count} exercises.
- Use only these exercise types when possible: translation, fill-in-the-blank, multiple-choice, sentence-building.
- Match the reference exercises' type mix, format, difficulty, and answer length.
- Do not duplicate the reference exercises or repeat the same structure unnecessarily.
- Each question must be concise, unambiguous, and answerable without outside knowledge unless the lesson topic requires it.
- Each expected_answer must be objectively checkable and written in {target_language}.
- For multiple-choice exercises, include all options in the question text and make exactly one option correct.
- Keep difficulty aligned with {level}; do not introduce grammar or vocabulary above that level.
"""

LISTENING_SCRIPT_RULES = """
Listening script quality rules:
- Write only in {target_language}.
- Match the requested CEFR level: use vocabulary, grammar, and sentence length appropriate for {level}.
- Keep the passage self-contained, concrete, and suitable for text-to-speech.
- Include enough explicit details to support multiple comprehension questions.
- Do not require outside knowledge.
- Avoid names, facts, or cultural details that could distract from the language goal unless the topic requires them.
"""

LISTENING_QUESTION_RULES = """
Listening question quality rules:
- Generate exactly {count} questions based only on the script.
- Write questions and options in {target_language}.
- Each question must have exactly four options.
- Exactly one option must have is_correct=true.
- Distractors must be plausible but clearly contradicted by, or unsupported by, the script.
- Do not ask about information that is not explicitly present in the script.
- Write each explanation in {target_language} and briefly cite the script detail that supports the answer.
"""

PRACTICE_CONVERSATION_RULES = """
Conversation quality rules:
- Stay fully in character as {scenario}.
- Respond only in {target_language}.
- Keep the reply appropriate for a {level} learner.
- Keep the conversation moving with one natural follow-up question when appropriate.
- Do not correct grammar or vocabulary during the conversation turn.
- If the learner says something unclear, ask a simple clarification question in character.
"""

SESSION_CORRECTION_RULES = """
Session correction rules:
- Analyse only user turns.
- Report only meaningful grammar, vocabulary, pronunciation-related phrasing, or word-order errors.
- Ignore minor speech-to-text artifacts such as missing punctuation, odd spacing, or one-letter slips when meaning is clear.
- Write corrected examples and explanations in {target_language}.
- Prefer the most useful corrections over a long exhaustive list.
- If there are no meaningful errors, return an empty corrections list.
"""

RAG_GROUNDING_RULES = """
Grounding rules:
- Use only the retrieved context.
- Do not invent facts, citations, filenames, or page numbers.
- If the context does not support an answer, say that the documents do not contain enough information.
- Cite source filenames and pages inline for factual claims.
- Keep the answer concise, practical, and directly tied to the user's question.
"""
