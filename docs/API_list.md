# API List

Status meanings:
- `done`: implemented and working in the current frontend flow.
- `unimplemented`: no real frontend integration path yet.
- `required fix`: integrated or expected, but still blocked by backend availability or known behavior issues.

| API | Status | Fixing part |
| --- | --- | --- |
| `POST /api/v1/auth/login` | `required fix` | Frontend call exists. Real backend user-service on `8081` was unavailable during testing, so sign-in could not be validated end to end. Fix: start/fix backend user-service connectivity and verify real login response flow. |
| `POST /api/v1/users` | `required fix` | Frontend registration call exists. Real backend user-service on `8081` was unavailable during testing, so account creation could not be validated end to end. Fix: start/fix backend user-service connectivity and verify real account creation flow. |
| `GET /api/v1/users/{user_id}/profile` | `required fix` | Frontend profile load exists. Real backend user-service on `8081` was unavailable during testing, so the UI can only surface the request error. Fix: restore backend profile endpoint and validate real data loading. |
| `PUT /api/v1/users/{user_id}/profile` | `required fix` | Frontend profile update exists. Real backend user-service on `8081` was unavailable during testing, so the UI can only surface the request error. Fix: restore backend profile endpoint and validate real save behavior. |
| `GET /api/v1/learning-plans/user/{user_id}` | `required fix` | Frontend plan loading exists. Learning-service on `8082` was unavailable during testing, so the frontend only showed empty states. Fix: restore backend plan endpoint and validate user-linked plans. |
| `POST /api/v1/learning-plans/default` | `unimplemented` | No current frontend action creates a default learning plan. Fix: add a frontend action in Start learning that calls this endpoint and refreshes the plan list. |
| `POST /api/v1/learning-plans/ai` | `required fix` | The frontend AI screen is intentionally disabled because backend learning-service and GenAI integration are not ready for a real flow. Fix: implement and validate backend AI-plan creation, then wire the frontend form submission. |
| `GET /api/v1/lessons/{lesson_id}` | `required fix` | Frontend lesson loading exists, but learning-service on `8082` was unavailable during testing. Fix: restore backend lesson endpoint and verify lesson detail rendering. |
| `POST /api/v1/lessons/{lesson_id}/ai-exercises` | `unimplemented` | No current frontend action requests AI exercises for a lesson. Fix: add a frontend lesson action only after backend learning-service/GenAI support is verified. |
| `POST /api/v1/answers` | `required fix` | Frontend answer submission exists, but progress-feedback-service on `8083` was unavailable during testing. Fix: restore backend answer endpoint and validate saved answers, score, and feedback refresh. |
| `GET /api/v1/answers/{answer_id}/feedback` | `unimplemented` | No dedicated frontend fetch path uses this endpoint yet. Fix: add a feedback detail fetch only if the UI needs a separate feedback retrieval flow. |
| `GET /api/v1/progress/user/{user_id}` | `required fix` | Frontend dashboard progress loading exists, but progress-feedback-service on `8083` was unavailable during testing. Fix: restore backend progress endpoint and validate dashboard aggregation. |