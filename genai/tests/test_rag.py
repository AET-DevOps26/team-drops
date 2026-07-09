from unittest.mock import patch

from langchain_core.messages import AIMessage
from langchain_core.runnables import RunnableLambda

from RAG import CorpusStats, RetrievedChunk
from app.schemas.rag import (
    RagLearningPlanExercise,
    RagLearningPlanLesson,
    RagLearningPlanResponse,
)
from tests.conftest import make_mock_structured_llm


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


def test_generate_rag_learning_plan_returns_structured_plan_and_sources(client, tmp_path):
    chunks = [
        RetrievedChunk(
            text="Practice concise interview answers with examples from your experience.",
            score=0.87,
            source="interview-guide.pdf",
            page=5,
            chunk_index=2,
        )
    ]
    llm_response = RagLearningPlanResponse(
        title="German Interview Readiness",
        description="A plan grounded in interview preparation material.",
        goal="Prepare for a German job interview",
        language="German",
        level="B1",
        duration="2 weeks",
        lessons=[
            RagLearningPlanLesson(
                title="Structured Interview Answers",
                topic="STAR interview answers",
                summary="Use situation, task, action, and result to organise answers.",
                order_number=1,
                content_blocks=["Explain STAR answers with examples from the retrieved guide."],
                exercises=[
                    RagLearningPlanExercise(
                        type="writing",
                        subtype="free_text",
                        question="Write a STAR answer for a teamwork question.",
                        expected_answer="A concise STAR answer with concrete actions and results.",
                        difficulty="B1",
                    )
                ],
            )
        ],
    )

    with patch("app.routers.rag._rag_doc_db", return_value=tmp_path), patch(
        "app.routers.rag.query_topic", return_value=chunks
    ), patch(
        "app.routers.rag.get_structured_llm",
        return_value=make_mock_structured_llm(llm_response),
    ):
        response = client.post(
            "/api/v1/genai/rag/learning-plan",
            json={
                "topic": "job interview",
                "learning_goal": "Prepare for a German job interview",
                "target_language": "German",
                "level": "B1",
                "duration_weeks": 2,
                "study_hours_per_week": 4,
                "minimum_lessons": 1,
                "maximum_lessons": 2,
                "exercise_types": ["writing"],
                "top_k": 1,
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["title"] == "German Interview Readiness"
    assert body["lessons"][0]["exercises"][0]["type"] == "writing"
    assert body["lessons"][0]["exercises"][0]["subtype"] == "free_text"
    assert body["sources"][0]["source"] == "interview-guide.pdf"


def test_generate_rag_learning_plan_rejects_wrong_lesson_count(client, tmp_path):
    llm_response = RagLearningPlanResponse(
        title="Too Short",
        description="Missing required lessons.",
        goal="Prepare",
        language="German",
        level="B1",
        duration="2 weeks",
        lessons=[
            RagLearningPlanLesson(
                title="Only Lesson",
                topic="Topic",
                summary="Summary",
                order_number=1,
                content_blocks=[],
                exercises=[
                    RagLearningPlanExercise(
                        type="writing",
                        subtype="free_text",
                        question="Question?",
                        expected_answer="Answer",
                        difficulty="B1",
                    )
                ],
            )
        ],
    )

    with patch("app.routers.rag._rag_doc_db", return_value=tmp_path), patch(
        "app.routers.rag.query_topic", return_value=[]
    ), patch(
        "app.routers.rag.get_structured_llm",
        return_value=make_mock_structured_llm(llm_response),
    ):
        response = client.post(
            "/api/v1/genai/rag/learning-plan",
            json={
                "topic": "job interview",
                "learning_goal": "Prepare",
                "target_language": "German",
                "level": "B1",
                "duration_weeks": 2,
                "study_hours_per_week": 4,
                "minimum_lessons": 2,
                "maximum_lessons": 3,
                "exercise_types": ["writing"],
            },
        )

    assert response.status_code == 502
    assert "lesson count outside requested range" in response.json()["message"]


def test_generate_rag_learning_plan_replaces_generic_normalized_metadata(client, tmp_path):
    chunks = [
        RetrievedChunk(
            text="The Grand Tour has scenic routes through Switzerland.",
            score=0.75,
            source="grand_tour_guide.pdf",
            page=4,
            chunk_index=0,
        )
    ]
    llm_response = RagLearningPlanResponse.model_validate(
        {
            "lessons": [
                {
                    "order_number": 1,
                    "content_blocks": ["Practice describing the Grand Tour route."],
                    "exercises": [
                        {
                            "type": "speaking",
                            "subtype": "speaking_prompt",
                            "description": "Describe the route in one minute.",
                        }
                    ],
                }
            ]
        }
    )

    with patch("app.routers.rag._rag_doc_db", return_value=tmp_path), patch(
        "app.routers.rag.query_topic", return_value=chunks
    ), patch(
        "app.routers.rag.get_structured_llm",
        return_value=make_mock_structured_llm(llm_response),
    ):
        response = client.post(
            "/api/v1/genai/rag/learning-plan",
            json={
                "topic": "Reisen in der Schweiz",
                "learning_goal": "Prepare for a German job interview",
                "target_language": "German",
                "level": "A2",
                "duration_weeks": 4,
                "study_hours_per_week": 5,
                "minimum_lessons": 1,
                "maximum_lessons": 2,
                "exercise_types": ["speaking"],
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["title"] == "Reisen in der Schweiz Learning Plan"
    assert body["goal"] == "Prepare for a German job interview"
    assert body["duration"] == "4 weeks"
    assert body["lessons"][0]["title"] == "Lesson 1: Reisen in der Schweiz"
    assert body["lessons"][0]["topic"] == "Reisen in der Schweiz"


def test_rag_learning_plan_schema_normalizes_live_llm_near_miss_shape():
    plan = RagLearningPlanResponse.model_validate(
        {
            "topic": "Reisen in der Schweiz",
            "learning_goal": "Prepare for a German job interview",
            "target_language": "German",
            "level": "A2",
            "duration_weeks": 4,
            "lessons": [
                {
                    "order_number": 1,
                    "content_blocks": [
                        {
                            "type": "text",
                            "content": "The Grand Tour is a Swiss travel route with useful planning tips.",
                        }
                    ],
                    "exercises": [
                        {
                            "type": "reading",
                            "subtype": "fill_in_blank",
                            "description": "The ticket costs ___ CHF.",
                            "answer": "20",
                        }
                    ],
                }
            ],
        }
    )

    assert plan.title == "Reisen in der Schweiz Learning Plan"
    assert plan.goal == "Prepare for a German job interview"
    assert plan.language == "German"
    assert plan.duration == "4 weeks"
    assert plan.lessons[0].title == "Lesson 1: Reisen in der Schweiz"
    assert plan.lessons[0].summary == "The Grand Tour is a Swiss travel route with useful planning tips."
    assert plan.lessons[0].content_blocks == [
        "The Grand Tour is a Swiss travel route with useful planning tips."
    ]
    assert plan.lessons[0].exercises[0].type == "writing"
    assert plan.lessons[0].exercises[0].question == "The ticket costs ___ CHF."
    assert plan.lessons[0].exercises[0].expected_answer == "20"
    assert plan.lessons[0].exercises[0].difficulty == "A2"
