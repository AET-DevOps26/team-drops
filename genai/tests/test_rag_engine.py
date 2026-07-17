import json

import pytest

from RAG import list_topics, query_topic


def _write_corpus(topic_dir, chunks, *, documents=None):
    corpus_dir = topic_dir / "corpus"
    corpus_dir.mkdir(parents=True)
    (corpus_dir / "chunks.json").write_text(
        json.dumps(
            {
                "topic": topic_dir.name,
                "documents": documents or [],
                "chunks": chunks,
            }
        ),
        encoding="utf-8",
    )


def test_query_topic_ranks_relevant_chunks_and_preserves_source_metadata(tmp_path):
    topic_dir = tmp_path / "job interview"
    topic_dir.mkdir()
    _write_corpus(
        topic_dir,
        [
            {
                "source": "behavioral-guide.pdf",
                "page": 12,
                "chunk_index": 4,
                "text": (
                    "For behavioral interview questions, structure the answer with the "
                    "STAR method: situation, task, action, and measurable result."
                ),
            },
            {
                "source": "travel-guide.pdf",
                "page": 3,
                "chunk_index": 1,
                "text": "Swiss mountain travel includes hiking routes and winter accommodation.",
            },
        ],
    )

    results = query_topic(
        tmp_path,
        "job interview",
        "How do I give a STAR behavioral interview answer with actions and results?",
        top_k=2,
    )

    assert len(results) == 2
    assert results[0].source == "behavioral-guide.pdf"
    assert results[0].page == 12
    assert results[0].chunk_index == 4
    assert results[0].score == 1.0


def test_query_topic_returns_empty_result_for_an_empty_built_corpus(tmp_path):
    topic_dir = tmp_path / "empty topic"
    topic_dir.mkdir()
    _write_corpus(topic_dir, [])

    assert query_topic(tmp_path, "empty topic", "anything") == []


@pytest.mark.parametrize("topic", ["../outside", "missing-topic"])
def test_query_topic_rejects_unsafe_or_unknown_topics(tmp_path, topic):
    expected_error = ValueError if topic == "../outside" else FileNotFoundError

    with pytest.raises(expected_error):
        query_topic(tmp_path, topic, "interview question")


def test_list_topics_only_returns_visible_directories(tmp_path):
    (tmp_path / "job interview").mkdir()
    (tmp_path / "travel").mkdir()
    (tmp_path / ".internal").mkdir()
    (tmp_path / "manifest.tsv").write_text("not a topic", encoding="utf-8")

    assert list_topics(tmp_path) == ["job interview", "travel"]
