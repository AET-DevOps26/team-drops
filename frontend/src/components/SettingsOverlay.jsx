import React from 'react';
import { Languages, LogOut, Moon, Sun, User, X } from 'lucide-react';

export function SettingsOverlay({
  closing,
  darkMode,
  language,
  onClose,
  onLanguage,
  onLogout,
  onProfile,
  onToggleDarkMode,
}) {
  return (
    <div className={`settings-overlay ${closing ? 'closing' : ''}`} role="presentation">
      <button
        className="settings-backdrop"
        type="button"
        aria-label="Close settings"
        onClick={() => onClose()}
      ></button>

      <aside className="settings-sidebar" aria-label="Settings sidebar">
        <header className="sidebar-topbar">
          <h2>Settings</h2>
          <button className="icon-button" type="button" aria-label="Close settings" onClick={onClose}>
            <X size={21} aria-hidden="true" />
          </button>
        </header>

        <div className="settings-list">
          <button className="settings-row" type="button" onClick={onProfile}>
            <span className="row-icon">
              <User size={20} aria-hidden="true" />
            </span>
            <span className="row-text">
              <strong>Personal profile</strong>
              <small>View and edit account details</small>
            </span>
          </button>

          <button className="settings-row" type="button" onClick={onLanguage}>
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
              {darkMode ? <Moon size={20} aria-hidden="true" /> : <Sun size={20} aria-hidden="true" />}
            </span>
            <span className="row-text">
              <strong>Light / dark mode</strong>
              <small>{darkMode ? 'Dark mode enabled' : 'Light mode enabled'}</small>
            </span>
            <button
              className={`switch ${darkMode ? 'on' : ''}`}
              type="button"
              aria-label="Toggle light and dark mode"
              onClick={onToggleDarkMode}
            >
              <span></span>
            </button>
          </div>
        </div>

        <button className="logout-button" type="button" onClick={onLogout}>
          <LogOut size={20} aria-hidden="true" />
          Log out
        </button>
      </aside>
    </div>
  );
}
