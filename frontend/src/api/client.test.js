import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../auth/keycloak', () => ({
  getValidAccessToken: vi.fn(),
}));

import { getValidAccessToken } from '../auth/keycloak';
import { getLearningPlans, submitAnswer } from './client';

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
});
