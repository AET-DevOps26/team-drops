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
            "Do not invent unsupported enum spellings or hyphenated values.",
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
            "summarise teachable material from the context.",
        ),
    ]
)
