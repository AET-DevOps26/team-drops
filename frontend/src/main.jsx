import React from 'react';
import { createRoot } from 'react-dom/client';
import { BookOpen, CalendarDays, Clock3, Mail, Settings, UserPlus } from 'lucide-react';
import './styles.css';

function App() {
  const [screen, setScreen] = React.useState('auth');
  const goToMain = () => setScreen('main');

  return (
    <main className="phone-stage">
      <div className="phone-frame" aria-label="Phone app preview">
        <div className="phone-speaker" aria-hidden="true"></div>

        <section className="app-shell" aria-label="APP_NAME">
          <div className="status-bar" aria-hidden="true">
            <span>9:41</span>
            <span>LTE 100%</span>
          </div>

          {screen === 'auth' ? (
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
                  <button className="auth-button gmail-button" type="button" onClick={goToMain}>
                    <span className="google-mark" aria-hidden="true">
                      G
                    </span>
                    Continue with Gmail
                  </button>

                  <button className="auth-button register-button" type="button" onClick={goToMain}>
                    <UserPlus size={20} aria-hidden="true" />
                    Register
                  </button>
                </div>

                <div className="divider">
                  <span>or</span>
                </div>

                <button className="text-login" type="button" onClick={goToMain}>
                  <Mail size={18} aria-hidden="true" />
                  Sign in with email
                </button>
              </section>
            </>
          ) : (
            <section className="main-page" aria-label="Main page">
              <header className="main-topbar">
                <div aria-hidden="true"></div>
                <button className="icon-button" type="button" aria-label="Settings">
                  <Settings size={22} aria-hidden="true" />
                </button>
              </header>

              <div className="main-background" aria-hidden="true"></div>

              <nav className="bottom-bar" aria-label="Main navigation">
                <button className="tab-button" type="button">
                  <CalendarDays size={21} aria-hidden="true" />
                  <span>Plan</span>
                </button>
                <button className="tab-button active" type="button">
                  <BookOpen size={21} aria-hidden="true" />
                  <span>Start learning</span>
                </button>
                <button className="tab-button" type="button">
                  <Clock3 size={21} aria-hidden="true" />
                  <span>History</span>
                </button>
              </nav>
            </section>
          )}

          <div className="home-indicator" aria-hidden="true"></div>
        </section>
      </div>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
