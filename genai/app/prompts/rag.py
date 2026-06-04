from langchain_core.prompts import ChatPromptTemplate

rag_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            "You are a helpful assistant answering questions using a RAG corpus. "
            "Use only the provided context. If the answer is not supported by the "
            "context, say that the documents do not contain enough information.",
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
