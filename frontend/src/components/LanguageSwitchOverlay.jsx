import React from 'react';
import { Languages, LoaderCircle } from 'lucide-react';

export function LanguageSwitchOverlay({ targetLanguage, t }) {
  return (
    <div
      className="language-switch-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="language-switch-title"
      aria-describedby="language-switch-description"
    >
      <section className="language-switch-card" aria-live="polite">
        <span className="language-switch-icon" aria-hidden="true">
          <Languages size={28} />
        </span>
        <LoaderCircle className="language-switch-spinner" size={34} aria-hidden="true" />
        <div>
          <h2 id="language-switch-title">{t.switchingLanguage}</h2>
          <p id="language-switch-description">
            {t.loadingLanguageContent.replace('{language}', targetLanguage)}
          </p>
        </div>
      </section>
    </div>
  );
}
