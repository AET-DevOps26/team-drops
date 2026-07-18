import React from 'react';
import {
  ArrowRight,
  Check,
  LoaderCircle,
  LogIn,
  MessageCircle,
  ShieldCheck,
  Sparkles,
  UserPlus,
} from 'lucide-react';

export function AuthPage({ t, authEnabled, authErrorMessage, authPending, onLogin, onRegister, onBypass }) {
  const [message, setMessage] = React.useState('');

  React.useEffect(() => {
    setMessage(authErrorMessage ?? '');
  }, [authErrorMessage]);

  if (authPending) {
    return (
      <section className="auth-processing" aria-live="polite" aria-busy="true">
        <div className="auth-processing-mark" aria-hidden="true">
          <LoaderCircle size={34} />
        </div>
        <div className="auth-processing-copy">
          <p className="eyebrow">{t.welcomeBack}</p>
          <h2>{t.authSigningIn}</h2>
        </div>
      </section>
    );
  }

  return (
    <div className="auth-page">
      <header className="app-header auth-brand-header">
        <div>
          <p className="app-label">{t.appLabel}</p>
          <h1>InterviewMate</h1>
        </div>
        <span className="auth-trust-badge">
          <ShieldCheck size={14} aria-hidden="true" />
          {t.authSecureShort}
        </span>
      </header>

      <section className="auth-card" aria-label="Sign in options">
        <div className="auth-hero-visual" aria-hidden="true">
          <div className="auth-visual-glow"></div>
          <div className="auth-spark auth-spark-one"><Sparkles size={16} /></div>
          <div className="auth-spark auth-spark-two"><Check size={16} /></div>
          <div className="auth-coach-bubble">
            <MessageCircle size={22} />
          </div>
          <div className="auth-question-card">
            <span>{t.authPracticeLabel}</span>
            <strong>{t.authPracticeQuestion}</strong>
            <div className="auth-answer-lines">
              <i></i>
              <i></i>
              <i></i>
            </div>
          </div>
          <div className="auth-score-pill">
            <Check size={14} />
            {t.authFeedbackReady}
          </div>
        </div>

        <div className="auth-hero-copy">
          <p className="eyebrow">{t.authEyebrow}</p>
          <h2>{t.authTitle}</h2>
          <p>{t.authDescription}</p>
        </div>

        <div className="auth-benefits" aria-label={t.authBenefitsLabel}>
          <span><Check size={14} aria-hidden="true" />{t.authBenefitPractice}</span>
          <span><Check size={14} aria-hidden="true" />{t.authBenefitFeedback}</span>
        </div>

        <div className="auth-entry-panel">
          {message && <p className="auth-error" role="alert">{message}</p>}

          <button
            className="auth-button auth-primary-button"
            disabled={authPending || !authEnabled}
            type="button"
            onClick={onLogin}
          >
            <LogIn size={19} aria-hidden="true" />
            <span>{t.authSignIn}</span>
            <ArrowRight className="auth-button-arrow" size={18} aria-hidden="true" />
          </button>

          <button
            className="auth-button auth-secondary-button"
            disabled={authPending || !authEnabled}
            type="button"
            onClick={onRegister}
          >
            <UserPlus size={19} aria-hidden="true" />
            {t.authCreateAccount}
          </button>

          {!authEnabled && (
            <button className="auth-dev-bypass" type="button" onClick={onBypass}>
              {t.authContinueWithout}
            </button>
          )}

          <p className="auth-security-note">
            <ShieldCheck size={14} aria-hidden="true" />
            {t.authSecure}
          </p>
        </div>
      </section>
    </div>
  );
}
