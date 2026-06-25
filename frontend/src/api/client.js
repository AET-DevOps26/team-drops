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
  genai: '/genai-service',
};

function buildHeaders(token, hasBody) {
  const headers = {
    Accept: 'application/json',
  };

  if (hasBody) {
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

async function request(service, path, { method = 'GET', token, body, signal } = {}) {
  let response;
  const resolvedToken = token ? await getValidAccessToken().then((refreshedToken) => refreshedToken ?? token) : token;

  try {
    response = await fetch(`${serviceBaseUrls[service]}${path}`, {
      method,
      headers: buildHeaders(resolvedToken, body !== undefined),
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (cause) {
    const error = new Error(`Unable to reach the ${service} service.`);
    error.code = 'BACKEND_UNAVAILABLE';
    error.cause = cause;
    throw error;
  }

  return parseResponse(response);
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

export function getLearningPlans(userId, token) {
  return request('learning', `/api/v1/learning-plans/user/${userId}`, { token });
}

export function createDefaultLearningPlan(payload, token) {
  return request('learning', '/api/v1/learning-plans/default', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function getLesson(lessonId, token) {
  return request('learning', `/api/v1/lessons/${lessonId}`, { token });
}

export function submitAnswer(payload, token) {
  return request('progress', '/api/v1/answers', {
    method: 'POST',
    token,
    body: payload,
  });
}

export function getUserAnswers(userId, token) {
  return request('progress', `/api/v1/answers/user/${userId}`, { token });
}

export function getFeedbackByAnswerId(answerId, token) {
  return request('progress', `/api/v1/answers/${answerId}/feedback`, { token });
}

export function getProgress(userId, token) {
  return request('progress', `/api/v1/progress/user/${userId}`, { token });
}

export function getRagTopics(token) {
  return request('genai', '/api/v1/genai/rag/topics', { token });
}

export function queryRag(payload, token, signal) {
  return request('genai', '/api/v1/genai/rag/query', {
    method: 'POST',
    token,
    body: payload,
    signal,
  });
}
