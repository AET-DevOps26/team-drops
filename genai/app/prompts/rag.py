from langchain_core.prompts import ChatPromptTemplate

from app.prompts.rubrics import RAG_GROUNDING_RULES

rag_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            "You are a helpful assistant answering questions using a RAG corpus. "
            f"{RAG_GROUNDING_RULES}",
        ),
        (
            "human",
            "Topic: {topic}\n\n"
            "Question:\n{question}\n\n"
            "Retrieved context:\n{context}\n\n"
            "Answer with concise, practical guidance. Cite source filenames and pages inline.",
        ),
    ]
)

rag_learning_plan_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            "You generate language-learning plans from a retrieved RAG corpus. "
            f"{RAG_GROUNDING_RULES} "
            "Return only the fields required by the structured output schema. "
            "Use learning-service-compatible exercise enum values only: "
            "types reading, writing, listening, speaking; "
            "subtypes translation, fill_in_blank, multiple_choice, sentence_building, "
            "free_text, speaking_prompt, listening_choice. "
            "Do not invent unsupported enum spellings or hyphenated values. "
            "Every exercise question must be self-contained and directly answerable by the learner. "
            "For fill_in_blank, include the full sentence with explicit ___ blanks. "
            "For multiple_choice and listening_choice, include all answer choices in the question text. "
            "For translation, include the source sentence to translate. "
            "For sentence_building, include the exact words or phrases to arrange. "
            "Use free_text when a task asks for an open written answer instead of a structured format.",
        ),
        (
            "human",
            "Topic: {topic}\n"
            "Learning goal: {learning_goal}\n"
            "Target language: {target_language}\n"
            "Level: {level}\n"
            "Duration weeks: {duration_weeks}\n"
            "Study hours per week: {study_hours_per_week}\n"
            "Lesson range: exactly between {minimum_lessons} and {maximum_lessons} lessons\n"
            "Requested exercise types: {exercise_types}\n\n"
            "Retrieved context:\n{context}\n\n"
            "Create a coherent persisted learning plan grounded only in the retrieved context. "
            "Set lesson order_number values starting at 1 without gaps. "
            "Every lesson must include at least one exercise and concise content_blocks that "
            "summarise teachable material from the context. "
            "Reject vague tasks like 'fill in the blanks with these terms' unless the question "
            "contains the actual blanks to fill.",
        ),
    ]
)
