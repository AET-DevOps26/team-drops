# Frontend Backend API Requirements

This document contains only the API requirements the frontend needs from the backend.

Main rule: the frontend must fetch user data, learning plans, lessons, progress, scores, answers, and feedback from backend services. The frontend should not call GenAI, Ollama, OpenAI, or any LLM provider directly. If AI is needed, the backend calls GenAI internally and returns normal backend API responses.

## Required Frontend Flow

The app should start with user login.

```text
1. User logs in
2. Frontend receives user ID and token/session
3. Frontend fetches user profile
4. Frontend fetches user learning plans
5. Frontend fetches dashboard progress
6. User opens a lesson
7. Frontend fetches lesson detail and exercises
8. User submits an answer
9. Backend stores answer, score, progress, and feedback
10. Frontend refreshes lesson/progress data from backend
```

The frontend should not depend on hardcoded learning plans once these APIs are implemented.

## Services

| Service | Port | Frontend Use |
| --- | ---: | --- |
| User Service | 8081 | Login, user account, profile |
| Learning Service | 8082 | Learning plans, lessons, exercises, AI-created plans/exercises |
| Progress-Feedback Service | 8083 | Submitted answers, scores, progress, feedback |
| GenAI Service | 8084 | Backend-internal only |

## Shared Requirements

### Authentication

After login, protected requests should include the token:

```http
Authorization: Bearer <access_token>
```

If JWT is not implemented yet, backend should still return a stable session/token string so the frontend has one consistent auth flow.

### IDs

Use integer IDs in all APIs:

```json
{
  "user_id": 1,
  "plan_id": 10,
  "lesson_id": 100,
  "exercise_id": 1000,
  "answer_id": 5000,
  "feedback_id": 7000
}
```

### Scores

Use `0..100` for all scores returned to the frontend.

```json
{
  "score": 86,
  "max_score": 100
}
```

If GenAI returns `0..10`, backend should convert it before saving and returning it.

### Status Values

Use these values for plans, lessons, and exercises:

```text
not-started
ongoing
finished
```

### Exercise Types

Frontend-level exercise types:

```text
reading
writing
listening
speaking
```

Backend can add a more specific subtype:

```json
{
  "type": "writing",
  "subtype": "translation"
}
```

Suggested subtype values:

```text
translation
fill_in_blank
multiple_choice
sentence_building
free_text
speaking_prompt
listening_choice
```

### Error Response

All services should use the same error format:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "email",
      "reason": "must be a valid email"
    }
  ]
}
```

## User Service API

### Create User

```http
POST /api/v1/users
```

Request:

```json
{
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "password": "secure-password"
}
```

Response `201`:

```json
{
  "id": 1,
  "name": "Ada Lovelace",
  "email": "ada@example.com"
}
```

### Login

```http
POST /api/v1/auth/login
```

The backend should look up the user in the user database by email, verify the password, and return the user identity plus token/session.

Request:

```json
{
  "email": "ada@example.com",
  "password": "secure-password"
}
```

Response `200`:

```json
{
  "user": {
    "id": 1,
    "name": "Ada Lovelace",
    "email": "ada@example.com"
  },
  "access_token": "jwt-or-session-token",
  "token_type": "Bearer"
}
```

Errors:

```text
400 invalid request
401 invalid email or password
404 user not found
```

### Get User Profile

```http
GET /api/v1/users/{user_id}/profile
```

Response `200`:

```json
{
  "id": 11,
  "user_id": 1,
  "name": "Ada Lovelace",
  "country": "Germany",
  "target_language": "German",
  "current_level": "A2",
  "learning_goal": "Prepare for a software engineering job interview"
}
```

### Upsert User Profile

```http
PUT /api/v1/users/{user_id}/profile
```

Request:

```json
{
  "name": "Ada Lovelace",
  "country": "Germany",
  "target_language": "German",
  "current_level": "A2",
  "learning_goal": "Prepare for a software engineering job interview"
}
```

Response `200`: same shape as `GET /api/v1/users/{user_id}/profile`.

## Learning Service API

All learning plans, lessons, exercises, generated AI plans, and generated AI exercises must be stored in backend database tables and fetched by the frontend.

### Get User Learning Plans

```http
GET /api/v1/learning-plans/user/{user_id}
```

Response `200`:

```json
[
  {
    "id": 10,
    "user_id": 1,
    "title": "Job Interview Preparation",
    "description": "A guided plan for interview communication.",
    "goal": "Prepare for software engineering interviews",
    "language": "German",
    "level": "A2",
    "duration": "2 weeks",
    "status": "ongoing",
    "progress": 45,
    "lessons": [
      {
        "id": 100,
        "plan_id": 10,
        "title": "Self Introduction",
        "topic": "Introduce yourself professionally in an interview.",
        "order_number": 1,
        "status": "ongoing",
        "progress": 50,
        "time_estimate_minutes": 8,
        "exercise_count": 4
      }
    ]
  }
]
```

### Create Default Learning Plan

```http
POST /api/v1/learning-plans/default
```

Request:

```json
{
  "user_id": 1,
  "target_language": "German",
  "current_level": "A2",
  "learning_goal": "Prepare for software engineering interviews"
}
```

Response `201`: one saved `LearningPlanResponse` with real backend IDs.

### Create AI Learning Plan

```http
POST /api/v1/learning-plans/ai
```

The frontend sends the learning goal and constraints. The Learning Service calls GenAI internally if needed, creates the plan, lessons, and exercises in the backend database, and returns the saved plan.

Request:

```json
{
  "user_id": 1,
  "target_language": "German",
  "current_level": "A2",
  "learning_goal": "Prepare for a German job interview with technical project questions.",
  "duration_weeks": 4,
  "study_hours_per_week": 5,
  "minimum_lessons": 6,
  "maximum_lessons": 12,
  "exercise_types": ["reading", "writing"]
}
```

Response `201`:

```json
{
  "id": 20,
  "user_id": 1,
  "title": "German Technical Interview Plan",
  "description": "A four-week plan for technical job interview language.",
  "goal": "Prepare for a German job interview with technical project questions.",
  "language": "German",
  "level": "A2",
  "duration": "4 weeks",
  "status": "not-started",
  "progress": 0,
  "lessons": [
    {
      "id": 201,
      "plan_id": 20,
      "title": "Introducing Technical Projects",
      "topic": "Explain one project clearly and professionally.",
      "order_number": 1,
      "status": "not-started",
      "progress": 0,
      "time_estimate_minutes": 12,
      "exercise_count": 4
    }
  ]
}
```

### Get Lesson Detail

```http
GET /api/v1/lessons/{lesson_id}
```

The lesson response should include the lesson content, exercises, stored status, progress, score, and any existing feedback references needed by the UI.

Response `200`:

```json
{
  "id": 100,
  "plan_id": 10,
  "title": "Self Introduction",
  "topic": "Introduce yourself professionally in an interview.",
  "order_number": 1,
  "status": "ongoing",
  "progress": 50,
  "time_estimate_minutes": 8,
  "content_blocks": [
    {
      "type": "content",
      "title": "Core idea",
      "subtitle": "8 min",
      "text": "A strong self-introduction is short, professional, and relevant.",
      "points": [
        "Use a three-part structure: identity, background, direction.",
        "Keep the first answer under one minute."
      ]
    },
    {
      "type": "exercise",
      "exercise_id": 1000
    },
    {
      "type": "summary",
      "title": "Lesson summary",
      "text": "You can now introduce yourself with a clear role and professional tone."
    }
  ],
  "exercises": [
    {
      "id": 1000,
      "lesson_id": 100,
      "type": "writing",
      "subtype": "free_text",
      "title": "Writing practice",
      "question": "Write a 4 sentence introduction for a software internship interview.",
      "expected_answer": "A concise introduction with identity, background, and motivation.",
      "difficulty": "A2",
      "status": "not-started",
      "score": null,
      "format": "Short written answer",
      "source": "default"
    }
  ]
}
```

### Generate AI Exercises For Lesson

```http
POST /api/v1/lessons/{lesson_id}/ai-exercises
```

The Learning Service should load the lesson, user profile, target language, level, and existing exercises from backend storage. It may call GenAI internally, then it must save generated exercises before returning them.

Request:

```json
{
  "user_id": 1,
  "count": 3,
  "exercise_types": ["reading", "writing"],
  "instructions": "Focus on technical project interview questions."
}
```

Response `201`:

```json
{
  "lesson_id": 100,
  "generated_count": 3,
  "exercises": [
    {
      "id": 1005,
      "lesson_id": 100,
      "type": "writing",
      "subtype": "translation",
      "title": "Translation practice",
      "question": "Translate: I contributed to the backend API.",
      "expected_answer": "Ich habe zur Backend-API beigetragen.",
      "difficulty": "A2",
      "status": "not-started",
      "score": null,
      "format": "Short written answer",
      "source": "ai"
    }
  ]
}
```

## Progress Feedback Service API

Answers, scores, progress records, and feedback must be stored in the backend. The frontend submits an answer and then renders the backend response.

### Submit Exercise Answer

```http
POST /api/v1/answers
```

Backend responsibilities:

- Validate `user_id` and `exercise_id`.
- Load exercise data from Learning Service or local read model.
- Save the submitted answer.
- Score objective exercises directly.
- For writing/free-text answers, call GenAI internally if needed.
- Save feedback if available.
- Update exercise, lesson, and plan progress.
- Return the saved answer, score, feedback, and updated progress values.

Request:

```json
{
  "user_id": 1,
  "exercise_id": 1000,
  "answer_text": "I am studying computer science and I enjoy building backend systems.",
  "client_context": {
    "lesson_id": 100,
    "plan_id": 10
  }
}
```

Response `200`:

```json
{
  "answer": {
    "id": 5000,
    "user_id": 1,
    "exercise_id": 1000,
    "answer_text": "I am studying computer science and I enjoy building backend systems.",
    "score": 86,
    "max_score": 100,
    "is_correct": true,
    "submitted_at": "2026-05-27T21:30:00Z"
  },
  "feedback": {
    "id": 7000,
    "answer_id": 5000,
    "message": "Good structure and relevant vocabulary. Add one more concrete project detail.",
    "weak_area": "specificity",
    "corrected_answer": "I am studying computer science, and I enjoy building backend systems that solve practical user problems.",
    "strengths": [
      "Clear main idea",
      "Relevant technical vocabulary"
    ],
    "improvements": [
      "Add one concrete project detail",
      "Use a more natural transition"
    ],
    "created_at": "2026-05-27T21:30:00Z"
  },
  "exercise_status": "finished",
  "lesson_progress": 75,
  "plan_progress": 48
}
```

### Get Feedback For Answer

```http
GET /api/v1/answers/{answer_id}/feedback
```

Response `200`:

```json
{
  "id": 7000,
  "answer_id": 5000,
  "message": "Good structure and relevant vocabulary.",
  "weak_area": "specificity",
  "corrected_answer": "I am studying computer science, and I enjoy building backend systems that solve practical user problems.",
  "strengths": ["Clear main idea"],
  "improvements": ["Add one concrete project detail"],
  "created_at": "2026-05-27T21:30:00Z"
}
```

### Get User Progress Summary

```http
GET /api/v1/progress/user/{user_id}
```

This endpoint feeds the dashboard. It should return stored and aggregated progress from backend data.

Response `200`:

```json
{
  "user_id": 1,
  "completed_exercises": 18,
  "total_exercises": 40,
  "average_score": 84,
  "current_plan": {
    "id": 10,
    "title": "Job Interview Preparation",
    "language": "German",
    "progress": 45
  },
  "recent_progress": [
    {
      "plan_id": 10,
      "lesson_id": 100,
      "lesson_title": "Self Introduction",
      "status": "ongoing",
      "progress": 75,
      "last_activity_at": "2026-05-27T21:30:00Z"
    }
  ],
  "recent_finished": [
    {
      "plan_id": 10,
      "lesson_id": 99,
      "lesson_title": "Education and Background",
      "finished_at": "2026-05-26T18:10:00Z",
      "score": 91
    }
  ],
  "top_lessons": [
    {
      "lesson_id": 99,
      "lesson_title": "Education and Background",
      "score": 91
    }
  ]
}
```

## Minimum Required Endpoints

```text
POST /api/v1/auth/login
POST /api/v1/users
GET  /api/v1/users/{user_id}/profile
PUT  /api/v1/users/{user_id}/profile

GET  /api/v1/learning-plans/user/{user_id}
POST /api/v1/learning-plans/default
POST /api/v1/learning-plans/ai
GET  /api/v1/lessons/{lesson_id}
POST /api/v1/lessons/{lesson_id}/ai-exercises

POST /api/v1/answers
GET  /api/v1/answers/{answer_id}/feedback
GET  /api/v1/progress/user/{user_id}
```

## Frontend Data Ownership Expectation

The frontend should treat backend data as source of truth:

- User identity comes from login response.
- Profile data comes from User Service.
- Learning plans come from Learning Service.
- Lessons and exercises come from Learning Service.
- AI-generated plans and exercises are created and stored by Learning Service.
- Submitted answers are stored by Progress-Feedback Service.
- Scores and feedback are stored by Progress-Feedback Service.
- Dashboard progress is fetched from Progress-Feedback Service.
- GenAI remains backend-internal.
