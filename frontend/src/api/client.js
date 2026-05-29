const serviceBaseUrls = {
  user: import.meta.env.VITE_USER_SERVICE_URL ?? '/user-service',
  learning: import.meta.env.VITE_LEARNING_SERVICE_URL ?? '/learning-service',
  progress: import.meta.env.VITE_PROGRESS_SERVICE_URL ?? '/progress-service',
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

export function getProgress(userId, token) {
  return request('progress', `/api/v1/progress/user/${userId}`, { token });
}
