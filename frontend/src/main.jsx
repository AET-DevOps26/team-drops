import React from 'react';
import { createRoot } from 'react-dom/client';
import { Mail, UserPlus } from 'lucide-react';
import './styles.css';

function App() {
  return (
    <main className="phone-stage">
      <div className="phone-frame" aria-label="Phone app preview">
        <div className="phone-speaker" aria-hidden="true"></div>

        <section className="app-shell" aria-label="APP_NAME login and registration">
          <div className="status-bar" aria-hidden="true">
            <span>9:41</span>
            <span>LTE 100%</span>
          </div>

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
              <button className="auth-button gmail-button" type="button">
                <span className="google-mark" aria-hidden="true">
                  G
                </span>
                Continue with Gmail
              </button>

              <button className="auth-button register-button" type="button">
                <UserPlus size={20} aria-hidden="true" />
                Register
              </button>
            </div>

            <div className="divider">
              <span>or</span>
            </div>

            <button className="text-login" type="button">
              <Mail size={18} aria-hidden="true" />
              Sign in with email
            </button>
          </section>

          <div className="home-indicator" aria-hidden="true"></div>
        </section>
      </div>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
