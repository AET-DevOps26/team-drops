import React from 'react';
import { ChevronLeft } from 'lucide-react';

export function ProfilePage({ profile, profileError, profilePending, t, onBack, onProfileChange }) {
  const [editing, setEditing] = React.useState(false);
  const [draft, setDraft] = React.useState(profile);

  React.useEffect(() => {
    setDraft(profile);
  }, [profile]);

  const updateField = (field, value) => {
    setDraft((currentDraft) => ({ ...currentDraft, [field]: value }));
  };

  const saveProfile = async () => {
    await onProfileChange({
      name: draft.name.trim() || profile.name,
      targetLanguage: draft.targetLanguage?.trim() || profile.targetLanguage,
    });
    setEditing(false);
  };

  return (
    <section className="profile-page" aria-label="Personal profile">
      <header className="page-topbar">
        <button className="icon-button" type="button" aria-label="Back to main page" onClick={onBack}>
          <ChevronLeft size={22} aria-hidden="true" />
        </button>
        <h2>{t.profile}</h2>
        <span aria-hidden="true"></span>
      </header>

      <div className="profile-content">
        {editing ? (
          <div className="profile-fields">
            <label className="profile-field">
              <span>{t.name}</span>
              <input
                maxLength={20}
                name="name"
                type="text"
                value={draft.name}
                onChange={(event) => updateField('name', event.target.value)}
              />
              <small>{draft.name.length}/20</small>
            </label>
            <label className="profile-field">
              <span>Target language</span>
              <input
                name="targetLanguage"
                type="text"
                value={draft.targetLanguage ?? ''}
                onChange={(event) => updateField('targetLanguage', event.target.value)}
              />
            </label>
          </div>
        ) : (
          <div className="profile-fields">
            <div className="profile-field">
              <span>{t.name}</span>
              <strong>{profile.name}</strong>
            </div>
            <div className="profile-field">
              <span>Target language</span>
              <strong>{profile.targetLanguage}</strong>
            </div>
          </div>
        )}

        {profileError && <p className="auth-error" role="alert">{profileError}</p>}

        <button
          className="edit-profile-button"
          disabled={profilePending}
          type="button"
          onClick={editing ? saveProfile : () => setEditing(true)}
        >
          {profilePending ? 'Saving...' : editing ? t.save : t.edit}
        </button>
      </div>
    </section>
  );
}
