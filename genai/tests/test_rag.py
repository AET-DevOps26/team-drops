import asyncio
from unittest.mock import patch

from langchain_core.messages import AIMessage
from langchain_core.runnables import RunnableLambda

from RAG import CorpusStats, RetrievedChunk
from app.schemas.rag import (
    RagLearningPlanExercise,
    RagLearningPlanLesson,
    RagLearningPlanQualityReview,
    RagLearningPlanResponse,
)
from tests.conftest import make_mock_structured_llm


def _structured_llm_sequence(*responses):
    response_iterator = iter(responses)
    return lambda _schema: make_mock_structured_llm(next(response_iterator))


def _accepted_review() -> RagLearningPlanQualityReview:
    return RagLearningPlanQualityReview(accepted=True)


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

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.build_corpus", return_value=stats),
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

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=chunks),
        patch("app.routers.rag.get_llm", return_value=llm),
    ):
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


def test_generate_rag_learning_plan_returns_structured_plan_and_sources(
    client, tmp_path
):
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
                content_blocks=[
                    "Explain STAR answers with examples from the retrieved guide."
                ],
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

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=chunks) as query_mock,
        patch(
            "app.routers.rag.get_structured_llm",
            side_effect=_structured_llm_sequence(llm_response, _accepted_review()),
        ),
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
    retrieval_query = query_mock.call_args.args[2]
    assert "STAR answers" in retrieval_query
    assert "technical explanations" in retrieval_query
    assert "role-play" in retrieval_query


def test_generate_rag_learning_plan_adds_missing_requested_exercise_types(
    client, tmp_path
):
    chunks = [
        RetrievedChunk(
            text="Practice concise software engineering interview answers.",
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
        duration="1 week",
        lessons=[
            RagLearningPlanLesson(
                title="Structured Interview Answers",
                topic="STAR interview answers",
                summary="Use situation, task, action, and result to organise answers.",
                order_number=1,
                content_blocks=[
                    "Explain STAR answers with examples from the retrieved guide."
                ],
                exercises=[
                    RagLearningPlanExercise(
                        type="writing",
                        subtype="free_text",
                        question="Write a STAR answer for a debugging question.",
                        expected_answer="A concise STAR answer with concrete actions and results.",
                        difficulty="B1",
                    )
                ],
            )
        ],
    )

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=chunks),
        patch(
            "app.routers.rag.get_structured_llm",
            side_effect=_structured_llm_sequence(llm_response, _accepted_review()),
        ),
    ):
        response = client.post(
            "/api/v1/genai/rag/learning-plan",
            json={
                "topic": "job interview",
                "learning_goal": "Prepare for software engineering interviews",
                "target_language": "German",
                "level": "B1",
                "duration_weeks": 1,
                "study_hours_per_week": 3,
                "minimum_lessons": 1,
                "maximum_lessons": 2,
                "exercise_types": ["reading", "listening", "writing", "speaking"],
                "top_k": 1,
            },
        )

    assert response.status_code == 200
    exercises = response.json()["lessons"][0]["exercises"]
    assert {exercise["type"] for exercise in exercises} == {
        "reading",
        "listening",
        "writing",
        "speaking",
    }
    assert any(
        exercise["subtype"] == "multiple_choice" and "A)" in exercise["question"]
        for exercise in exercises
    )


def test_generate_rag_learning_plan_removes_unrequested_exercise_types(
    client, tmp_path
):
    chunks = [
        RetrievedChunk(
            text="Practice concise answers.",
            score=0.87,
            source="interview-guide.pdf",
            page=5,
            chunk_index=2,
        )
    ]
    llm_response = RagLearningPlanResponse(
        title="Writing Only",
        description="A writing plan.",
        goal="Prepare",
        language="German",
        level="B1",
        duration="1 week",
        lessons=[
            RagLearningPlanLesson(
                title="Lesson",
                topic="Topic",
                summary="Summary",
                order_number=1,
                content_blocks=["Summary"],
                exercises=[
                    RagLearningPlanExercise(
                        type="speaking",
                        subtype="speaking_prompt",
                        question="Speak about the topic.",
                        expected_answer="A spoken answer.",
                        difficulty="B1",
                    ),
                    RagLearningPlanExercise(
                        type="writing",
                        subtype="free_text",
                        question="Write about the topic.",
                        expected_answer="A written answer.",
                        difficulty="B1",
                    ),
                ],
            ),
            RagLearningPlanLesson(
                title="Lesson Without Requested Types",
                topic="Topic",
                summary="Summary",
                order_number=2,
                content_blocks=["Summary"],
                exercises=[
                    RagLearningPlanExercise(
                        type="speaking",
                        subtype="speaking_prompt",
                        question="Speak about the second topic.",
                        expected_answer="A spoken answer.",
                        difficulty="B1",
                    ),
                ],
            ),
        ],
    )

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=chunks),
        patch(
            "app.routers.rag.get_structured_llm",
            side_effect=_structured_llm_sequence(llm_response, _accepted_review()),
        ),
    ):
        response = client.post(
            "/api/v1/genai/rag/learning-plan",
            json={
                "topic": "job interview",
                "learning_goal": "Prepare",
                "target_language": "German",
                "level": "B1",
                "duration_weeks": 1,
                "study_hours_per_week": 3,
                "minimum_lessons": 1,
                "maximum_lessons": 2,
                "exercise_types": ["writing"],
                "top_k": 1,
            },
        )

    assert response.status_code == 200
    lessons = response.json()["lessons"]
    assert len(lessons) == 2
    assert [exercise["type"] for exercise in lessons[0]["exercises"]] == ["writing"]
    assert [exercise["type"] for exercise in lessons[1]["exercises"]] == ["writing"]
    assert "Prepare" in lessons[1]["exercises"][0]["question"]


def test_generate_rag_learning_plan_rejects_unknown_exercise_type(client):
    response = client.post(
        "/api/v1/genai/rag/learning-plan",
        json={
            "topic": "job interview",
            "learning_goal": "Prepare",
            "target_language": "German",
            "level": "B1",
            "duration_weeks": 1,
            "study_hours_per_week": 3,
            "minimum_lessons": 1,
            "maximum_lessons": 1,
            "exercise_types": ["grammar"],
            "top_k": 1,
        },
    )

    assert response.status_code == 422


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

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=[]),
        patch(
            "app.routers.rag.get_structured_llm",
            side_effect=_structured_llm_sequence(llm_response),
        ),
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


def test_generate_rag_learning_plan_times_out_llm_generation(
    client, tmp_path, monkeypatch
):
    chunks = [
        RetrievedChunk(
            text="Practice concise interview answers.",
            score=0.87,
            source="interview-guide.pdf",
            page=5,
            chunk_index=2,
        )
    ]

    async def slow_response(_):
        await asyncio.sleep(0.05)
        return {}

    monkeypatch.setattr("app.config.settings.llm_request_timeout_seconds", 0.01)

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=chunks),
        patch(
            "app.routers.rag.get_structured_llm",
            return_value=RunnableLambda(slow_response),
        ),
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
                "minimum_lessons": 1,
                "maximum_lessons": 1,
                "exercise_types": ["writing"],
            },
        )

    assert response.status_code == 504
    assert "timed out" in response.json()["message"]


def test_generate_rag_learning_plan_replaces_generic_normalized_metadata(
    client, tmp_path
):
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

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=chunks) as query_mock,
        patch(
            "app.routers.rag.get_structured_llm",
            side_effect=_structured_llm_sequence(llm_response, _accepted_review()),
        ),
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
    assert "STAR answers" not in query_mock.call_args.args[2]


def test_job_interview_plan_replaces_attire_speaking_exercise(client, tmp_path):
    chunks = [
        RetrievedChunk(
            text="Use STAR to answer behavioral interview questions with concrete examples.",
            score=1.0,
            source="interview-guide.pdf",
            page=3,
            chunk_index=1,
        )
    ]
    rejected_plan = _speaking_plan(
        "Explain what you should wear to an interview.",
        "Describe suitable professional attire.",
    )
    repaired_plan = _speaking_plan(
        "Tell me about a time you resolved a conflict in your team.",
        "Use STAR and explain the situation, your action, and the result.",
    )

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=chunks),
        patch(
            "app.routers.rag.get_structured_llm",
            side_effect=_structured_llm_sequence(
                rejected_plan,
                _accepted_review(),
                repaired_plan,
                _accepted_review(),
            ),
        ) as structured_llm,
    ):
        response = client.post(
            "/api/v1/genai/rag/learning-plan",
            json=_learning_plan_request(),
        )

    assert response.status_code == 200
    assert structured_llm.call_count == 4
    question = response.json()["lessons"][0]["exercises"][0]["question"]
    assert question == "Tell me about a time you resolved a conflict in your team."
    assert "wear" not in question.lower()


def test_job_interview_plan_returns_last_plan_after_configured_corrective_attempts(
    client, tmp_path
):
    rejected_plan = _speaking_plan(
        "Explain what you should wear to an interview.",
        "Describe suitable professional attire.",
    )
    last_rejected_plan = _speaking_plan(
        "Explain why you should arrive early for an interview.",
        "Describe an appropriate arrival time.",
    )

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=[]),
        patch("app.routers.rag.settings.rag_learning_plan_max_repair_attempts", 1),
        patch(
            "app.routers.rag.get_structured_llm",
            side_effect=_structured_llm_sequence(
                rejected_plan,
                _accepted_review(),
                last_rejected_plan,
                _accepted_review(),
            ),
        ) as structured_llm,
    ):
        response = client.post(
            "/api/v1/genai/rag/learning-plan",
            json=_learning_plan_request(),
        )

    assert response.status_code == 200
    assert structured_llm.call_count == 4
    question = response.json()["lessons"][0]["exercises"][0]["question"]
    assert question == "Explain why you should arrive early for an interview."


def test_job_interview_plan_can_succeed_on_second_corrective_attempt(client, tmp_path):
    rejected_plan = _speaking_plan(
        "Explain what you should wear to an interview.",
        "Describe suitable professional attire.",
    )
    repaired_plan = _speaking_plan(
        "Tell me about a time you resolved a conflict in your team.",
        "Use STAR and explain the situation, your action, and the result.",
    )

    with (
        patch("app.routers.rag._rag_doc_db", return_value=tmp_path),
        patch("app.routers.rag.query_topic", return_value=[]),
        patch("app.routers.rag.settings.rag_learning_plan_max_repair_attempts", 2),
        patch(
            "app.routers.rag.get_structured_llm",
            side_effect=_structured_llm_sequence(
                rejected_plan,
                _accepted_review(),
                rejected_plan,
                _accepted_review(),
                repaired_plan,
                _accepted_review(),
            ),
        ) as structured_llm,
    ):
        response = client.post(
            "/api/v1/genai/rag/learning-plan",
            json=_learning_plan_request(),
        )

    assert response.status_code == 200
    assert structured_llm.call_count == 6
    question = response.json()["lessons"][0]["exercises"][0]["question"]
    assert question == "Tell me about a time you resolved a conflict in your team."


def _learning_plan_request() -> dict:
    return {
        "topic": "job interview",
        "learning_goal": "Prepare for a German software engineering interview",
        "target_language": "German",
        "level": "B1",
        "duration_weeks": 2,
        "study_hours_per_week": 4,
        "minimum_lessons": 1,
        "maximum_lessons": 1,
        "exercise_types": ["speaking"],
    }


def _speaking_plan(question: str, expected_answer: str) -> RagLearningPlanResponse:
    return RagLearningPlanResponse(
        title="Interview Speaking Practice",
        description="Practice spoken interview responses.",
        goal="Prepare for a German software engineering interview",
        language="German",
        level="B1",
        duration="2 weeks",
        lessons=[
            RagLearningPlanLesson(
                title="Interview response practice",
                topic="Interview responses",
                summary="Practise responding clearly in an interview.",
                order_number=1,
                content_blocks=["Structure concise spoken responses."],
                exercises=[
                    RagLearningPlanExercise(
                        type="speaking",
                        subtype="speaking_prompt",
                        question=question,
                        expected_answer=expected_answer,
                        difficulty="B1",
                    )
                ],
            )
        ],
    )


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
                            "content": (
                                "The Grand Tour is a Swiss travel route with useful planning tips."
                            ),
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
    assert plan.lessons[0].summary == (
        "The Grand Tour is a Swiss travel route with useful planning tips."
    )
    assert plan.lessons[0].content_blocks == [
        "The Grand Tour is a Swiss travel route with useful planning tips."
    ]
    assert plan.lessons[0].exercises[0].type == "writing"
    assert plan.lessons[0].exercises[0].question == "The ticket costs ___ CHF."
    assert plan.lessons[0].exercises[0].expected_answer == "20"
    assert plan.lessons[0].exercises[0].difficulty == "A2"


def test_rag_learning_plan_schema_repairs_empty_fields_and_unknown_subtype():
    plan = RagLearningPlanResponse.model_validate(
        {
            "topic": "Reisen in der Schweiz",
            "learning_goal": "Plan a Swiss trip",
            "target_language": "French",
            "level": "B1",
            "duration_weeks": 3,
            "title": "",
            "description": "",
            "goal": "",
            "language": "",
            "duration": "",
            "lessons": [
                {
                    "title": "",
                    "topic": "",
                    "summary": "",
                    "content_blocks": [{"content": "Useful travel phrases."}],
                    "exercises": [
                        {
                            "subtype": "surprise_mode",
                            "expected_answer": "Bonjour",
                        }
                    ],
                }
            ],
        }
    )

    assert plan.title == "Reisen in der Schweiz Learning Plan"
    assert plan.description == "A RAG-grounded learning plan for Plan a Swiss trip."
    assert plan.goal == "Plan a Swiss trip"
    assert plan.language == "French"
    assert plan.duration == "3 weeks"
    assert plan.lessons[0].order_number == 1
    assert plan.lessons[0].title == "Lesson 1: Reisen in der Schweiz"
    assert plan.lessons[0].topic == "Reisen in der Schweiz"
    assert plan.lessons[0].summary == "Useful travel phrases."
    assert plan.lessons[0].exercises[0].type == "writing"
    assert plan.lessons[0].exercises[0].subtype == "free_text"
    assert plan.lessons[0].exercises[0].question == (
        "Write a short French answer grounded in the lesson content."
    )


def test_rag_learning_plan_schema_repairs_unknown_subtype_to_compatible_writing():
    exercise = RagLearningPlanExercise.model_validate(
        {
            "type": "reading",
            "subtype": "surprise_mode",
            "question": "Answer this question.",
            "expected_answer": "An answer.",
            "difficulty": "B1",
        }
    )

    assert exercise.type == "writing"
    assert exercise.subtype == "free_text"


def test_rag_learning_plan_schema_normalizes_common_subtype_alias():
    exercise = RagLearningPlanExercise.model_validate(
        {
            "type": "reading",
            "subtype": "multiple-choice",
            "question": "Choose one.\nA) One\nB) Two\nC) Three\nD) Four",
            "expected_answer": "A",
            "difficulty": "B1",
        }
    )

    assert exercise.type == "reading"
    assert exercise.subtype == "multiple_choice"


def test_rag_learning_plan_schema_repairs_fill_blank_without_blanks():
    exercise = RagLearningPlanExercise.model_validate(
        {
            "type": "writing",
            "subtype": "fill_in_blank",
            "question": (
                "Ergänze die Lücken mit den passenden deutschen Begriffen "
                "(z. B. Anzug, Bluse, geschlossene Schuhe)."
            ),
            "expected_answer": "Anzug, Bluse, geschlossene Schuhe",
            "difficulty": "A2",
        }
    )

    assert exercise.type == "writing"
    assert exercise.subtype == "free_text"
    assert exercise.question == (
        "Write a short German answer using these terms: Anzug, Bluse, geschlossene Schuhe."
    )


def test_rag_learning_plan_schema_repairs_multiple_choice_without_options():
    exercise = RagLearningPlanExercise.model_validate(
        {
            "type": "reading",
            "subtype": "multiple_choice",
            "question": "Welcher Tipp gehört zu einem One-Way-Video-Interview?",
            "expected_answer": "B) Blickkontakt zur Webcam halten",
            "difficulty": "A2",
        }
    )

    assert exercise.type == "writing"
    assert exercise.subtype == "free_text"
    assert exercise.question == (
        "Answer this question in German: Welcher Tipp gehört zu einem One-Way-Video-Interview?"
    )


def test_rag_learning_plan_schema_repairs_partial_multiple_choice_options():
    exercise = RagLearningPlanExercise.model_validate(
        {
            "type": "reading",
            "subtype": "multiple_choice",
            "question": "Choose the best answer.\nA) Only one option",
            "expected_answer": "A",
            "difficulty": "B1",
        }
    )

    assert exercise.type == "writing"
    assert exercise.subtype == "free_text"
    assert (
        exercise.question
        == "Answer this question in German: Choose the best answer.\nA) Only one option"
    )


def test_rag_learning_plan_schema_keeps_newline_multiple_choice_options():
    question = "Welche Vorbereitung ist nicht empfohlen?\nA) Ruhiger Platz\nB) Stimme variieren\nC) Laut sprechen\nD) Lebenslauf lesen"
    exercise = RagLearningPlanExercise.model_validate(
        {
            "type": "reading",
            "subtype": "multiple_choice",
            "question": question,
            "expected_answer": "C",
            "difficulty": "B1",
        }
    )

    assert exercise.type == "reading"
    assert exercise.subtype == "multiple_choice"
    assert exercise.question == question


def test_rag_learning_plan_schema_downgrades_bare_choice_label_to_open_question():
    exercise = RagLearningPlanExercise.model_validate(
        {
            "type": "reading",
            "subtype": "multiple_choice",
            "question": "Which answer is best?",
            "expected_answer": "C",
            "difficulty": "B1",
        }
    )

    assert exercise.type == "writing"
    assert exercise.subtype == "free_text"
    assert exercise.question == "Answer this question in German: Which answer is best?"
    assert "A) C" not in exercise.question


def test_rag_learning_plan_schema_keeps_listening_choice_without_inline_options():
    exercise = RagLearningPlanExercise.model_validate(
        {
            "type": "listening",
            "subtype": "listening_choice",
            "question": "Generate a listening passage about explaining a backend API trade-off.",
            "expected_answer": "Select the most accurate listening response.",
            "difficulty": "B1",
        }
    )

    assert exercise.type == "listening"
    assert exercise.subtype == "listening_choice"
    assert (
        exercise.question
        == "Generate a listening passage about explaining a backend API trade-off."
    )
