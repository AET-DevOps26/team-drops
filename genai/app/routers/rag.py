# ruff: noqa: E402
import asyncio
from pathlib import Path
import sys

from fastapi import APIRouter, HTTPException

for parent in Path(__file__).resolve().parents:
    if (parent / "RAG").is_dir() and str(parent) not in sys.path:
        sys.path.insert(0, str(parent))
        break

from RAG import build_corpus, list_topics, query_topic
from app.config import settings
from app.llm import get_llm
from app.prompts.rag import rag_prompt
from app.schemas.rag import (
    RagCorpusResponse,
    RagQueryRequest,
    RagQueryResponse,
    RagSource,
    RagTopicsResponse,
)  # noqa: E402

router = APIRouter(prefix="/rag", tags=["rag"])


@router.get(
    "/topics",
    operation_id="listRagTopics",
    response_model=RagTopicsResponse,
    summary="List available RAG topics",
    openapi_extra={"x-service": "genai-service"},
)
async def list_rag_topics() -> RagTopicsResponse:
    return RagTopicsResponse(topics=list_topics(_rag_doc_db()))


@router.post(
    "/topics/{topic}/corpus",
    operation_id="buildRagCorpus",
    response_model=RagCorpusResponse,
    summary="Build or rebuild a RAG topic corpus",
    description=(
        "Reads PDFs from `RAG doc DB/{topic}` and writes corpus files under "
        "`RAG doc DB/{topic}/corpus`."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def build_rag_corpus(topic: str) -> RagCorpusResponse:
    try:
        stats = await asyncio.to_thread(build_corpus, _rag_doc_db(), topic)
    except (FileNotFoundError, ValueError) as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=500, detail=f"Failed to build RAG corpus: {exc}"
        ) from exc

    return RagCorpusResponse(
        topic=stats.topic,
        pdf_count=stats.pdf_count,
        chunk_count=stats.chunk_count,
        corpus_dir=str(stats.corpus_dir),
    )


@router.post(
    "/query",
    operation_id="queryRag",
    response_model=RagQueryResponse,
    summary="Answer a question using a RAG topic",
    description=(
        "Retrieves context from PDFs under `RAG doc DB/{topic}`. If the corpus is "
        "missing or stale, it is rebuilt automatically under that topic's `corpus` folder."
    ),
    openapi_extra={"x-service": "genai-service"},
)
async def query_rag(body: RagQueryRequest) -> RagQueryResponse:
    try:
        chunks = await asyncio.to_thread(
            query_topic,
            _rag_doc_db(),
            body.topic,
            body.question,
            top_k=body.top_k,
            rebuild=body.rebuild_corpus,
        )
    except (FileNotFoundError, ValueError) as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"RAG retrieval failed: {exc}") from exc

    context = _format_context(chunks)
    llm = get_llm()
    chain = rag_prompt | llm

    try:
        result = await chain.ainvoke(
            {"topic": body.topic, "question": body.question, "context": context}
        )
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"LLM invocation failed: {exc}"
        ) from exc

    return RagQueryResponse(
        topic=body.topic,
        question=body.question,
        answer=str(getattr(result, "content", result)),
        sources=[
            RagSource(
                source=chunk.source,
                page=chunk.page,
                chunk_index=chunk.chunk_index,
                score=chunk.score,
                text=chunk.text,
            )
            for chunk in chunks
        ],
    )


def _rag_doc_db() -> Path:
    return Path(settings.rag_doc_db_path)


def _format_context(chunks) -> str:
    if not chunks:
        return "No relevant context was retrieved."
    return "\n\n".join(
        f"[{chunk.source}, page {chunk.page}, chunk {chunk.chunk_index}]\n{chunk.text}"
        for chunk in chunks
    )
