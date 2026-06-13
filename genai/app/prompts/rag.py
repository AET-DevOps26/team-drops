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
