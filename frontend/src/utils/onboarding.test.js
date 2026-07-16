import { beforeEach, describe, expect, it } from 'vitest';
import {
  beginRegistrationIntro,
  cancelRegistrationIntro,
  completeIntro,
  shouldShowIntro,
} from './onboarding';

describe('guided intro onboarding state', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('shows for a user created by the current login request', () => {
    expect(shouldShowIntro(localStorage, { id: 42, new_user: true })).toBe(true);
    expect(shouldShowIntro(localStorage, { id: 42, new_user: false })).toBe(true);
  });

  it('marks completion per user without suppressing another new user', () => {
    shouldShowIntro(localStorage, { id: 42, new_user: true });
    completeIntro(localStorage, 42);

    expect(shouldShowIntro(localStorage, { id: 42, new_user: false })).toBe(false);
    expect(shouldShowIntro(localStorage, { id: 84, new_user: true })).toBe(true);
  });

  it('uses registration intent as fallback and clears cancelled login intent', () => {
    beginRegistrationIntro(localStorage);
    expect(shouldShowIntro(localStorage, { id: 42, new_user: false })).toBe(true);

    beginRegistrationIntro(localStorage);
    cancelRegistrationIntro(localStorage);
    expect(shouldShowIntro(localStorage, { id: 84, new_user: false })).toBe(false);
  });
});
