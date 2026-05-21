import React from 'react';
import { Mail, UserPlus } from 'lucide-react';

export function AuthPage({ onAuthenticated }) {
  return (
    <>
      <header className="app-header">
        <div className="logo-placeholder" aria-label="Logo placeholder">
          TD
        </div>
        <div>
          <p className="app-label">Language learning</p>
          <h1>APP_NAME</h1>
        </div>
      </header>

      <section className="auth-card" aria-label="Sign in options">
        <div className="auth-heading">
          <p className="eyebrow">Welcome back</p>
          <h2>Sign in to continue learning.</h2>
        </div>

        <div className="auth-actions">
          <button className="auth-button gmail-button" type="button" onClick={onAuthenticated}>
            <span className="google-mark" aria-hidden="true">
              G
            </span>
            Continue with Gmail
          </button>

          <button className="auth-button register-button" type="button" onClick={onAuthenticated}>
            <UserPlus size={20} aria-hidden="true" />
            Register
          </button>
        </div>

        <div className="divider">
          <span>or</span>
        </div>

        <button className="text-login" type="button" onClick={onAuthenticated}>
          <Mail size={18} aria-hidden="true" />
          Sign in with email
        </button>
      </section>
    </>
  );
}
