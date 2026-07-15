import { getValidAccessToken } from '../auth/keycloak';

// Always use relative service paths from the browser so requests go through
// the dev server proxy (or the same-origin server in production). Avoid
// embedding Docker-internal hostnames into client-side code because the
// browser (running on the developer machine) cannot resolve container DNS
// names like "user-service".
const serviceBaseUrls = {
  user: '/user-service',
  learning: '/learning-service',
  progress: '/progress-service',
};

function buildHeaders(token, hasJsonBody) {
  const headers = {
    Accept: 'application/json',
  };

  if (hasJsonBody) {
    headers['Content-Type'] = 'application/json';
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') ?? '';
  const isJson = contentType.includes('application/json');
  const payload = isJson ? await response.json() : null;

  if (response.ok) {
    return payload;
  }

  const error = new Error(payload?.message ?? `Request failed with status ${response.status}`);
  error.status = response.status;
  error.payload = payload;
  throw error;
}

async function request(service, path, { method = 'GET', token, body } = {}) {
  let response;
  const resolvedToken = token ? await getValidAccessToken().then((refreshedToken) => refreshedToken ?? token) : token;
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
  const hasJsonBody = body !== undefined && !isFormData;

  try {
    response = await fetch(`${serviceBaseUrls[service]}${path}`, {
      method,
      headers: buildHeaders(resolvedToken, hasJsonBody),
      body: body === undefined || isFormData ? body : JSON.stringify(body),
    });
  } catch (cause) {
    const error = new Error(`Unable to reach the ${service} service.`);
    error.code = 'BACKEND_UNAVAILABLE';
    error.cause = cause;
    throw error;
  }

  return parseResponse(response);
}

function languageQuery(language) {
  return language ? `?language=${encodeURIComponent(language)}` : '';
}

function scopedProgressQuery({ planId, targetLanguage } = {}) {
  const params = new URLSearchParams();

  if (planId != null) {
    params.set('plan_id', planId);
  }

  if (targetLanguage) {
    params.set('target_language', targetLanguage);
  }

  const query = params.toString();
  return query ? `?${query}` : '';
}

export function getCurrentUser(token) {
  return request('user', '/api/v1/users/me', { token });
}

export function getUserProfile(userId, token) {
  return request('user', `/api/v1/users/${userId}/profile`, { token });
}

export function updateUserProfile(userId, profile, token) {
  return request('user', `/api/v1/users/${userId}/profile`, {
    method: 'PUT',
    token,
    body: profile,
  });
}

export function getLearningPlans(userId, token, language) {
  return request('learning', `/api/v1/learning-plans/user/${userId}${languageQuery(language)}`, { token });
}

export function createDefaultLearningPlan(payload, token) {
  return request('learning', '/api/v1/learning-plans/default', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function createAiLearningPlan(payload, token) {
  return request('learning', '/api/v1/learning-plans/ai', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function getLesson(lessonId, token, language) {
  return request('learning', `/api/v1/lessons/${lessonId}${languageQuery(language)}`, { token });
}

export function submitAnswer(payload, token) {
  return request('progress', '/api/v1/answers', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function submitSpeakingAnswer(payload, token) {
  const formData = new FormData();
  const audioName = payload.audio?.name || 'speaking-answer.webm';

  formData.append('audio', payload.audio, audioName);
  formData.append('user_id', String(payload.user_id));
  formData.append('exercise_id', String(payload.exercise_id));
  formData.append('lesson_id', String(payload.lesson_id));

  if (payload.plan_id !== undefined && payload.plan_id !== null) {
    formData.append('plan_id', String(payload.plan_id));
  }

  if (payload.target_language) {
    formData.append('target_language', payload.target_language);
  }

  if (payload.level) {
    formData.append('level', payload.level);
  }

  return request('progress', '/api/v1/answers/speaking', {
    method: 'POST',
    token,
    body: formData,
  });
}

export function getUserAnswers(userId, token, scope) {
  return request('progress', `/api/v1/answers/user/${userId}${scopedProgressQuery(scope)}`, { token });
}

export function getFeedbackByAnswerId(answerId, token) {
  return request('progress', `/api/v1/answers/${answerId}/feedback`, { token });
}

export function getProgress(userId, token, scope) {
  return request('progress', `/api/v1/progress/user/${userId}${scopedProgressQuery(scope)}`, { token });
}

export function generateListeningContent(exerciseId, lessonId, targetLanguage, level, token) {
  return request('progress', '/api/v1/listening/generate', {
    method: 'POST',
    token,
    body: { exercise_id: exerciseId, lesson_id: lessonId, target_language: targetLanguage, level },
  });
}
