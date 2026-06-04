from __future__ import annotations

import json
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from langchain_community.retrievers import BM25Retriever
from langchain_core.documents import Document
from llama_index.core.node_parser import SentenceSplitter
from llama_index.core.schema import Document as LlamaDocument
from pypdf import PdfReader

DEFAULT_CHUNK_SIZE = 900
DEFAULT_CHUNK_OVERLAP = 160
TOKEN_RE = re.compile(r"[a-zA-Z][a-zA-Z0-9']+")
STOPWORDS = {
    "a",
    "about",
    "and",
    "are",
    "as",
    "for",
    "from",
    "have",
    "in",
    "is",
    "of",
    "on",
    "or",
    "that",
    "the",
    "to",
    "with",
    "you",
    "your",
    "auf",
    "aus",
    "das",
    "der",
    "die",
    "ein",
    "eine",
    "für",
    "im",
    "in",
    "ist",
    "mit",
    "und",
    "von",
    "zu",
}


@dataclass(frozen=True)
class CorpusStats:
    topic: str
    topic_dir: Path
    corpus_dir: Path
    pdf_count: int
    chunk_count: int


@dataclass(frozen=True)
class RetrievedChunk:
    text: str
    score: float
    source: str
    page: int
    chunk_index: int


def list_topics(rag_doc_db: Path) -> list[str]:
    root = rag_doc_db.resolve()
    if not root.exists():
        return []
    return sorted(
        child.name
        for child in root.iterdir()
        if child.is_dir() and not child.name.startswith(".")
    )


def build_corpus(
    rag_doc_db: Path,
    topic: str,
    *,
    chunk_size: int = DEFAULT_CHUNK_SIZE,
    chunk_overlap: int = DEFAULT_CHUNK_OVERLAP,
) -> CorpusStats:
    topic_dir = _resolve_topic_dir(rag_doc_db, topic)
    pdfs = _topic_pdfs(topic_dir)
    if not pdfs:
        raise FileNotFoundError(f"No PDF documents found for RAG topic '{topic}'")

    chunks: list[dict[str, Any]] = []
    for pdf_path in pdfs:
        chunks.extend(_extract_pdf_chunks(pdf_path, chunk_size, chunk_overlap))

    corpus_dir = topic_dir / "corpus"
    corpus_dir.mkdir(parents=True, exist_ok=True)
    payload = {
        "topic": topic,
        "chunk_size": chunk_size,
        "chunk_overlap": chunk_overlap,
        "documents": [_document_fingerprint(path) for path in pdfs],
        "chunks": chunks,
    }
    (corpus_dir / "chunks.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return CorpusStats(topic, topic_dir, corpus_dir, len(pdfs), len(chunks))


def query_topic(
    rag_doc_db: Path,
    topic: str,
    question: str,
    *,
    top_k: int = 5,
    rebuild: bool = False,
) -> list[RetrievedChunk]:
    topic_dir = _resolve_topic_dir(rag_doc_db, topic)
    if rebuild or _corpus_is_stale(topic_dir):
        build_corpus(rag_doc_db, topic)

    corpus = _load_corpus(topic_dir)
    chunks = corpus.get("chunks", [])
    if not chunks:
        return []

    documents = [
        Document(
            page_content=chunk["text"],
            metadata={
                "source": chunk["source"],
                "page": chunk["page"],
                "chunk_index": chunk["chunk_index"],
            },
        )
        for chunk in chunks
        if chunk.get("text")
    ]
    if not documents:
        return []

    retriever = BM25Retriever.from_documents(documents)
    retriever.k = top_k
    results = retriever.invoke(question)

    retrieved: list[RetrievedChunk] = []
    for rank, document in enumerate(results, start=1):
        metadata = document.metadata
        retrieved.append(
            RetrievedChunk(
                text=document.page_content,
                score=round(1 / rank, 6),
                source=str(metadata.get("source", "unknown.pdf")),
                page=int(metadata.get("page") or 1),
                chunk_index=int(metadata.get("chunk_index") or 0),
            )
        )
    return retrieved


def _resolve_topic_dir(rag_doc_db: Path, topic: str) -> Path:
    if not topic.strip():
        raise ValueError("RAG topic must not be empty")
    topic_path = Path(topic)
    if topic_path.is_absolute() or ".." in topic_path.parts:
        raise ValueError("RAG topic must be a folder name under the document DB")

    root = rag_doc_db.resolve()
    topic_dir = (root / topic_path).resolve()
    if root not in topic_dir.parents and topic_dir != root:
        raise ValueError("RAG topic resolves outside the document DB")
    if not topic_dir.exists() or not topic_dir.is_dir():
        raise FileNotFoundError(f"RAG topic folder not found: {topic}")
    return topic_dir


def _topic_pdfs(topic_dir: Path) -> list[Path]:
    return sorted(
        path
        for path in topic_dir.glob("*.pdf")
        if path.is_file() and not path.name.startswith(".")
    )


def _extract_pdf_chunks(
    pdf_path: Path, chunk_size: int, chunk_overlap: int
) -> list[dict[str, Any]]:
    documents = _load_pdf_pages(pdf_path)
    splitter = SentenceSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
    nodes = splitter.get_nodes_from_documents(documents)

    chunks: list[dict[str, Any]] = []
    for node in nodes:
        metadata = node.metadata or {}
        text = _normalize_text(node.get_content())
        if not _is_useful_text(text):
            continue
        chunks.append(
            {
                "source": Path(str(metadata.get("file_name") or pdf_path.name)).name,
                "page": _metadata_page(metadata),
                "chunk_index": len(chunks),
                "text": text,
            }
        )
    return chunks


def _load_pdf_pages(pdf_path: Path) -> list[LlamaDocument]:
    reader = PdfReader(str(pdf_path))
    documents: list[LlamaDocument] = []
    for page_number, page in enumerate(reader.pages, start=1):
        text = _normalize_text(page.extract_text() or "")
        if not text:
            continue
        documents.append(
            LlamaDocument(
                text=text,
                metadata={"file_name": pdf_path.name, "page_label": page_number},
            )
        )
    return documents


def _normalize_text(text: str) -> str:
    cleaned = "".join(
        char
        if char in "\n\t" or not unicodedata.category(char).startswith("C")
        else " "
        for char in text
    )
    return re.sub(r"\s+", " ", cleaned).strip()


def _is_useful_text(text: str) -> bool:
    if len(text) < 80:
        return False
    lower = f" {text.lower()} "
    if any(
        marker in lower
        for marker in (
            " endobj ",
            " /structparent ",
            " /subtype ",
            " /contents ",
            " xref ",
        )
    ):
        return False
    printable_ascii = sum(32 <= ord(char) <= 126 for char in text)
    if printable_ascii / max(len(text), 1) < 0.85:
        return False
    tokens = [match.group(0).lower() for match in TOKEN_RE.finditer(text)]
    if len(tokens) < 15:
        return False
    stopword_count = sum(token in STOPWORDS for token in tokens)
    if stopword_count < 5:
        return False
    return any(
        marker in lower
        for marker in (
            " interview",
            " employer",
            " career",
            " question",
            " job ",
            " resume",
            " preparation",
            " schweiz",
            " reisen",
            " reise",
            " wandern",
            " wanderung",
            " ausflug",
            " tourismus",
            " unterkunft",
            " sommer",
            " winter",
            " sehenswürdigkeit",
        )
    )


def _metadata_page(metadata: dict[str, Any]) -> int:
    page = metadata.get("page_label") or metadata.get("page_number") or metadata.get("page")
    try:
        return int(page)
    except (TypeError, ValueError):
        return 1


def _load_corpus(topic_dir: Path) -> dict[str, Any]:
    corpus_path = topic_dir / "corpus" / "chunks.json"
    if not corpus_path.exists():
        raise FileNotFoundError(f"Corpus has not been built for topic '{topic_dir.name}'")
    return json.loads(corpus_path.read_text(encoding="utf-8"))


def _corpus_is_stale(topic_dir: Path) -> bool:
    corpus_path = topic_dir / "corpus" / "chunks.json"
    if not corpus_path.exists():
        return True

    try:
        corpus = json.loads(corpus_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return True

    current = [_document_fingerprint(path) for path in _topic_pdfs(topic_dir)]
    return corpus.get("documents") != current


def _document_fingerprint(path: Path) -> dict[str, Any]:
    stat = path.stat()
    return {
        "filename": path.name,
        "size": stat.st_size,
        "modified_ns": stat.st_mtime_ns,
    }
