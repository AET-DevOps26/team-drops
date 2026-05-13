import React from 'react';
import { ChevronLeft, User } from 'lucide-react';

export function ProfilePage({ onBack }) {
  return (
    <section className="profile-page" aria-label="Personal profile">
      <header className="page-topbar">
        <button className="icon-button" type="button" aria-label="Back to main page" onClick={onBack}>
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
  );
}
