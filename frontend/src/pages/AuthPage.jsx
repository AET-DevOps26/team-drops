import React from 'react';
import { LogIn, UserPlus } from 'lucide-react';

export function AuthPage({ t, authEnabled, authErrorMessage, authPending, onLogin, onRegister, onBypass }) {
  const [message, setMessage] = React.useState('');

  React.useEffect(() => {
    setMessage(authErrorMessage ?? '');
  }, [authErrorMessage]);

  return (
    <>
      <header className="app-header">
        <button
          className="logo-placeholder logo-bypass-button"
          disabled={authEnabled}
          type="button"
          aria-label="Bypass login"
          onClick={onBypass}
        >
          TD
        </button>
        <div>
          <p className="app-label">{t.appLabel}</p>
          <h1>InterviewMate</h1>
        </div>
      </header>

      <section className="auth-card" aria-label="Sign in options">
        <div className="auth-heading">
          <p className="eyebrow">{t.welcomeBack}</p>
          <h2>Sign in to continue learning.</h2>
        </div>

        <div className="auth-actions">
          <button className="auth-button register-button" disabled={authPending || !authEnabled} type="button" onClick={onRegister}>
            <UserPlus size={20} aria-hidden="true" />
            Register
          </button>
        </div>

        <div className="divider">
          <span>or</span>
        </div>

        {message && <p className="auth-error" role="alert">{message}</p>}

        <button className="text-login" disabled={authPending || !authEnabled} type="button" onClick={onLogin}>
          <LogIn size={18} aria-hidden="true" />
          Sign in with Keycloak
        </button>

        {!authEnabled && (
          <button className="text-login" type="button" onClick={onBypass}>
            Continue without authentication
          </button>
        )}
      </section>
    </>
  );
}
