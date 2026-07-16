import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../auth/keycloak', () => ({
  getValidAccessToken: vi.fn(),
  redirectToLoginForExpiredSession: vi.fn(),
}));

import { getValidAccessToken, redirectToLoginForExpiredSession } from '../auth/keycloak';
import { getLearningPlans, getLesson, submitAnswer, submitSpeakingAnswer } from './client';

function jsonResponse(payload, init = {}) {
  return new Response(JSON.stringify(payload), {
    status: init.status ?? 200,
    headers: { 'content-type': 'application/json' },
  });
}

describe('API client integration behavior', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    globalThis.fetch = vi.fn();
  });

  it('sends refreshed bearer tokens and encoded query parameters', async () => {
    getValidAccessToken.mockResolvedValue('fresh-token');
    fetch.mockResolvedValue(jsonResponse([{ id: 1, title: 'German interview plan' }]));

    const plans = await getLearningPlans(42, 'stale-token', 'German A2');

    expect(plans).toHaveLength(1);
    expect(fetch).toHaveBeenCalledWith(
      '/learning-service/api/v1/learning-plans/user/42?language=German%20A2',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({
          Accept: 'application/json',
          Authorization: 'Bearer fresh-token',
        }),
      }),
    );
  });

  it('serializes JSON answers and surfaces backend validation errors', async () => {
    getValidAccessToken.mockResolvedValue(null);
    fetch.mockResolvedValue(jsonResponse({ message: 'Answer text is required' }, { status: 400 }));

    await expect(submitAnswer({ exercise_id: 7, answer_text: '' }, 'token')).rejects.toMatchObject({
      message: 'Answer text is required',
      status: 400,
      payload: { message: 'Answer text is required' },
    });

    expect(fetch).toHaveBeenCalledWith(
      '/progress-service/api/v1/answers',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          Authorization: 'Bearer token',
        }),
        body: JSON.stringify({ exercise_id: 7, answer_text: '' }),
      }),
    );
  });

  it('converts network failures into service-specific availability errors', async () => {
    fetch.mockRejectedValue(new TypeError('failed to fetch'));

    await expect(getLearningPlans(42)).rejects.toMatchObject({
      message: 'Unable to reach the learning service.',
      code: 'BACKEND_UNAVAILABLE',
    });
  });

  it('redirects to login instead of surfacing an expired-session error', async () => {
    getValidAccessToken.mockResolvedValue('expired-token');
    redirectToLoginForExpiredSession.mockResolvedValue();
    fetch.mockResolvedValue(jsonResponse({ message: 'Unauthorized' }, { status: 401 }));

    await expect(getLearningPlans(42, 'expired-token')).resolves.toBeNull();

    expect(redirectToLoginForExpiredSession).toHaveBeenCalledOnce();
  });

  it('submits speaking metadata with the new target_language multipart field', async () => {
    getValidAccessToken.mockResolvedValue('fresh-token');
    fetch.mockResolvedValue(jsonResponse({ transcription: 'Hello' }));
    const audio = new File(['audio-bytes'], 'answer.webm', { type: 'audio/webm' });

    await submitSpeakingAnswer({
      audio,
      user_id: 42,
      exercise_id: 7,
      lesson_id: 3,
      plan_id: 1,
      target_language: 'English',
      level: 'A2',
    }, 'stale-token');

    const [url, options] = fetch.mock.calls[0];
    expect(url).toBe('/progress-service/api/v1/answers/speaking');
    expect(options.method).toBe('POST');
    expect(options.headers).toEqual(expect.objectContaining({
      Accept: 'application/json',
      Authorization: 'Bearer fresh-token',
    }));
    expect(options.headers).not.toHaveProperty('Content-Type');
    expect(options.body).toBeInstanceOf(FormData);
    expect(options.body.get('audio')).toBeInstanceOf(File);
    expect(options.body.get('user_id')).toBe('42');
    expect(options.body.get('exercise_id')).toBe('7');
    expect(options.body.get('lesson_id')).toBe('3');
    expect(options.body.get('plan_id')).toBe('1');
    expect(options.body.get('target_language')).toBe('English');
    expect(options.body.get('level')).toBe('A2');
  });

  it('preserves the selected language when reloading a lesson', async () => {
    fetch.mockResolvedValue(jsonResponse({ id: 3 }));

    await getLesson(3, null, 'German');

    expect(fetch).toHaveBeenCalledWith(
      '/learning-service/api/v1/lessons/3?language=German',
      expect.objectContaining({ method: 'GET' }),
    );
  });
});
