from __future__ import annotations

from pydantic import BaseModel, Field


class RagQueryRequest(BaseModel):
    topic: str = Field(
        ...,
        description="Folder name under RAG doc DB, e.g. 'job interview'",
        examples=["job interview"],
    )
    question: str = Field(
        ...,
        min_length=1,
        description="Question to answer from the topic corpus",
        examples=["How should I answer behavioral interview questions?"],
    )
    top_k: int = Field(
        default=5,
        ge=1,
        le=12,
        description="Number of retrieved chunks to pass to the LLM",
    )
    rebuild_corpus: bool = Field(
        default=False,
        description="Force rebuilding corpus files before retrieval",
    )


class RagSource(BaseModel):
    source: str
    page: int
    chunk_index: int
    score: float
    text: str


class RagQueryResponse(BaseModel):
    topic: str
    question: str
    answer: str
    sources: list[RagSource]


class RagCorpusResponse(BaseModel):
    topic: str
    pdf_count: int
    chunk_count: int
    corpus_dir: str


class RagTopicsResponse(BaseModel):
    topics: list[str]
