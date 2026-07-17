import json
from unittest.mock import patch

import pytest
from langchain_core.messages import AIMessage
from langchain_core.runnables import RunnableLambda


@pytest.mark.e2e
def test_rag_query_crosses_fastapi_real_retrieval_and_response_mapping(
    client, tmp_path
):
    topic_dir = tmp_path / "job interview"
    corpus_dir = topic_dir / "corpus"
    corpus_dir.mkdir(parents=True)
    (corpus_dir / "chunks.json").write_text(
        json.dumps(
            {
                "topic": "job interview",
                "documents": [],
                "chunks": [
                    {
                        "source": "behavioral-guide.pdf",
                        "page": 8,
                        "chunk_index": 2,
                        "text": (
                            "Answer behavioral interview questions with the STAR method. "
                            "Describe the situation, task, action, and measurable result."
                        ),
                    },
                    {
                        "source": "travel-guide.pdf",
                        "page": 1,
                        "chunk_index": 0,
                        "text": "Mountain travel includes hiking routes and winter accommodation.",
                    },
                    {
                        "source": "language-guide.pdf",
                        "page": 2,
                        "chunk_index": 1,
                        "text": "Language practice develops vocabulary and listening comprehension.",
                    },
                    {
                        "source": "project-guide.pdf",
                        "page": 5,
                        "chunk_index": 3,
                        "text": "Project planning defines milestones, ownership, and delivery dates.",
                    },
                    {
                        "source": "technical-guide.pdf",
                        "page": 4,
                        "chunk_index": 2,
                        "text": "Technical documentation describes system architecture and operations.",
                    },
                ],
            }
        ),
        encoding="utf-8",
    )
    llm = RunnableLambda(
        lambda _: AIMessage(content="Use STAR and give a measurable result.")
    )

    with patch("app.routers.rag._rag_doc_db", return_value=tmp_path), patch(
        "app.routers.rag.get_llm", return_value=llm
    ):
        response = client.post(
            "/api/v1/genai/rag/query",
            json={
                "topic": "job interview",
                "question": "How should I answer a STAR behavioral interview question?",
                "top_k": 1,
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["topic"] == "job interview"
    assert (
        body["question"] == "How should I answer a STAR behavioral interview question?"
    )
    assert body["answer"] == "Use STAR and give a measurable result."
    assert body["sources"] == [
        {
            "source": "behavioral-guide.pdf",
            "page": 8,
            "chunk_index": 2,
            "score": 1.0,
            "text": (
                "Answer behavioral interview questions with the STAR method. "
                "Describe the situation, task, action, and measurable result."
            ),
        }
    ]
