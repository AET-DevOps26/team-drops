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

async function request(service, path, { method = 'GET', token, body } = {}) {
  let response;

  try {
    response = await fetch(`${serviceBaseUrls[service]}${path}`, {
      method,
      headers: buildHeaders(token, body !== undefined),
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (cause) {
    const error = new Error(`Unable to reach the ${service} service.`);
    error.code = 'BACKEND_UNAVAILABLE';
    error.cause = cause;
    throw error;
  }

  return parseResponse(response);
}

export function login(credentials) {
  return request('user', '/api/v1/auth/login', {
    method: 'POST',
    body: credentials,
  });
}

export function createUser(payload) {
  return request('user', '/api/v1/users', {
    method: 'POST',
    body: payload,
  });
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
