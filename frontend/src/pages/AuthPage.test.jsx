import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it, vi } from 'vitest';
import { AuthPage } from './AuthPage';

const copy = {
  appLabel: 'Language learning',
  authEyebrow: 'Your personal interview coach',
  authTitle: 'Practice with purpose. Interview with confidence.',
  authDescription: 'Build stronger answers with guided speaking practice.',
  authPracticeLabel: 'Practice question',
  authPracticeQuestion: 'Tell me about a project you are proud of.',
  authFeedbackReady: 'Feedback ready',
  authBenefitsLabel: 'InterviewMate benefits',
  authBenefitPractice: 'Real interview practice',
  authBenefitFeedback: 'Instant feedback',
  authSignIn: 'Sign in',
  authCreateAccount: 'Create account',
  authContinueWithout: 'Continue without authentication',
  authSecure: 'Secure authentication with Keycloak',
  authSecureShort: 'Secure',
};

function renderAuth(overrides = {}) {
  return renderToStaticMarkup(
    <AuthPage
      authEnabled
      authErrorMessage=""
      authPending={false}
      t={copy}
      onBypass={vi.fn()}
      onLogin={vi.fn()}
      onRegister={vi.fn()}
      {...overrides}
    />,
  );
}

describe('AuthPage', () => {
  it('presents clear sign-in and account creation actions', () => {
    const markup = renderAuth();

    expect(markup).toContain(copy.authTitle);
    expect(markup).toContain(copy.authSignIn);
    expect(markup).toContain(copy.authCreateAccount);
    expect(markup).toContain(copy.authBenefitFeedback);
    expect(markup).not.toContain(copy.authContinueWithout);
  });

  it('keeps the sign-in page visible while authentication is pending', () => {
    const markup = renderAuth({ authPending: true });

    expect(markup).toContain(copy.authTitle);
    expect(markup).toContain(copy.authCreateAccount);
    expect(markup).toContain('disabled=""');
    expect(markup).not.toContain('aria-busy="true"');
  });

  it('only exposes the development bypass when authentication is disabled', () => {
    const markup = renderAuth({ authEnabled: false });

    expect(markup).toContain(copy.authContinueWithout);
    expect(markup.match(/Continue without authentication/g)).toHaveLength(1);
    expect(markup).toContain('disabled=""');
  });
});
