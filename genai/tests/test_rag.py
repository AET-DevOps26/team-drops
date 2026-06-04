from unittest.mock import patch

from langchain_core.messages import AIMessage
from langchain_core.runnables import RunnableLambda

from RAG import CorpusStats, RetrievedChunk


def test_list_rag_topics(client, tmp_path):
    (tmp_path / "job interview").mkdir()
    (tmp_path / "other topic").mkdir()

    with patch("app.routers.rag._rag_doc_db", return_value=tmp_path):
        response = client.get("/api/v1/genai/rag/topics")

    assert response.status_code == 200
    assert response.json() == {"topics": ["job interview", "other topic"]}


def test_build_rag_corpus_returns_stats(client, tmp_path):
    stats = CorpusStats(
        topic="job interview",
        topic_dir=tmp_path / "job interview",
        corpus_dir=tmp_path / "job interview" / "corpus",
        pdf_count=20,
        chunk_count=333,
    )

    with patch("app.routers.rag._rag_doc_db", return_value=tmp_path), patch(
        "app.routers.rag.build_corpus", return_value=stats
    ):
        response = client.post("/api/v1/genai/rag/topics/job interview/corpus")

    assert response.status_code == 200
    assert response.json()["pdf_count"] == 20
    assert response.json()["chunk_count"] == 333


def test_query_rag_returns_answer_and_sources(client, tmp_path):
    chunks = [
        RetrievedChunk(
            text="Use the STAR method to structure behavioral interview answers.",
            score=1.0,
            source="interview-guide.pdf",
            page=3,
            chunk_index=7,
        )
    ]
    llm = RunnableLambda(lambda _: AIMessage(content="Use STAR and cite examples."))

    with patch("app.routers.rag._rag_doc_db", return_value=tmp_path), patch(
        "app.routers.rag.query_topic", return_value=chunks
    ), patch("app.routers.rag.get_llm", return_value=llm):
        response = client.post(
            "/api/v1/genai/rag/query",
            json={
                "topic": "job interview",
                "question": "How should I answer behavioral interview questions?",
                "top_k": 1,
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["answer"] == "Use STAR and cite examples."
    assert body["sources"][0]["source"] == "interview-guide.pdf"
    assert body["sources"][0]["page"] == 3
