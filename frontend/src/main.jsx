import React from 'react';
import { createRoot } from 'react-dom/client';
import {
  BookOpen,
  CalendarDays,
  ChevronLeft,
  Clock3,
  Languages,
  LogOut,
  Mail,
  Moon,
  Settings,
  Sun,
  User,
  UserPlus,
  X,
} from 'lucide-react';
import './styles.css';

function App() {
  const [screen, setScreen] = React.useState('auth');
  const [settingsOpen, setSettingsOpen] = React.useState(false);
  const [settingsClosing, setSettingsClosing] = React.useState(false);
  const [darkMode, setDarkMode] = React.useState(false);
  const [language, setLanguage] = React.useState('English');

  const openSettings = () => {
    setSettingsClosing(false);
    setSettingsOpen(true);
  };

  const closeSettings = (nextScreen) => {
    setSettingsClosing(true);
    window.setTimeout(() => {
      setSettingsOpen(false);
      setSettingsClosing(false);

      if (nextScreen) {
        setScreen(nextScreen);
      }
    }, 220);
  };

  const goToMain = () => {
    setScreen('main');
    setSettingsOpen(false);
    setSettingsClosing(false);
  };
  const logOut = () => {
    closeSettings('auth');
  };
  const openProfile = () => {
    closeSettings('profile');
  };
  const openLanguage = () => {
    closeSettings('language');
  };

  const languages = ['English', 'German', 'Spanish', 'French', 'Italian', 'Japanese'];

  return (
    <main className="phone-stage">
      <div className="phone-frame" aria-label="Phone app preview">
        <div className="phone-speaker" aria-hidden="true"></div>

        <section className={`app-shell ${darkMode ? 'dark-mode' : ''}`} aria-label="APP_NAME">
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
          ) : screen === 'main' ? (
            <section className="main-page" aria-label="Main page">
              <header className="main-topbar">
                <div aria-hidden="true"></div>
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Settings"
                  onClick={openSettings}
                >
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
          ) : screen === 'profile' ? (
            <section className="profile-page" aria-label="Personal profile">
              <header className="page-topbar">
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Back to main page"
                  onClick={goToMain}
                >
                  <ChevronLeft size={22} aria-hidden="true" />
                </button>
                <h2>Profile</h2>
                <span aria-hidden="true"></span>
              </header>

              <div className="profile-content">
                <div className="profile-avatar" aria-label="Profile picture placeholder">
                  <User size={38} aria-hidden="true" />
                </div>
                <button className="change-photo-button" type="button">
                  Change photo
                </button>

                <div className="profile-fields">
                  <div className="profile-field">
                    <span>Name</span>
                    <strong>APP_USER</strong>
                  </div>
                  <div className="profile-field">
                    <span>Country</span>
                    <strong>Germany</strong>
                  </div>
                </div>

                <button className="edit-profile-button" type="button">
                  Edit
                </button>
              </div>
            </section>
          ) : (
            <section className="language-page" aria-label="Language selection">
              <header className="page-topbar">
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Back to main page"
                  onClick={goToMain}
                >
                  <ChevronLeft size={22} aria-hidden="true" />
                </button>
                <h2>Language</h2>
                <span aria-hidden="true"></span>
              </header>

              <div className="language-content">
                <p className="language-heading">Choose app language</p>
                <div className="language-bubbles">
                  {languages.map((item) => (
                    <button
                      className={`language-bubble ${language === item ? 'selected' : ''}`}
                      key={item}
                      type="button"
                      onClick={() => setLanguage(item)}
                    >
                      {item}
                    </button>
                  ))}
                </div>
              </div>
            </section>
          )}

          {settingsOpen && (
            <div
              className={`settings-overlay ${settingsClosing ? 'closing' : ''}`}
              role="presentation"
            >
              <button
                className="settings-backdrop"
                type="button"
                aria-label="Close settings"
                onClick={() => closeSettings()}
              ></button>

              <aside className="settings-sidebar" aria-label="Settings sidebar">
                <header className="sidebar-topbar">
                  <h2>Settings</h2>
                  <button
                    className="icon-button"
                    type="button"
                    aria-label="Close settings"
                    onClick={() => closeSettings()}
                  >
                    <X size={21} aria-hidden="true" />
                  </button>
                </header>

                <div className="settings-list">
                  <button className="settings-row" type="button" onClick={openProfile}>
                    <span className="row-icon">
                      <User size={20} aria-hidden="true" />
                    </span>
                    <span className="row-text">
                      <strong>Personal profile</strong>
                      <small>View and edit account details</small>
                    </span>
                  </button>

                  <button className="settings-row" type="button" onClick={openLanguage}>
                    <span className="row-icon">
                      <Languages size={20} aria-hidden="true" />
                    </span>
                    <span className="row-text">
                      <strong>Language</strong>
                      <small>{language}</small>
                    </span>
                  </button>

                  <div className="settings-row">
                    <span className="row-icon">
                      {darkMode ? (
                        <Moon size={20} aria-hidden="true" />
                      ) : (
                        <Sun size={20} aria-hidden="true" />
                      )}
                    </span>
                    <span className="row-text">
                      <strong>Light / dark mode</strong>
                      <small>{darkMode ? 'Dark mode enabled' : 'Light mode enabled'}</small>
                    </span>
                    <button
                      className={`switch ${darkMode ? 'on' : ''}`}
                      type="button"
                      aria-label="Toggle light and dark mode"
                      onClick={() => setDarkMode((value) => !value)}
                    >
                      <span></span>
                    </button>
                  </div>
                </div>

                <button className="logout-button" type="button" onClick={logOut}>
                  <LogOut size={20} aria-hidden="true" />
                  Log out
                </button>
              </aside>
            </div>
          )}

          <div className="home-indicator" aria-hidden="true"></div>
        </section>
      </div>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
