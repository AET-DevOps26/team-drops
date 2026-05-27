import React from 'react';
import { ChevronLeft } from 'lucide-react';

export function LanguagePage({ heading, language, languages, title, onBack, onLanguage }) {
  return (
    <section className="language-page" aria-label="Language selection">
      <header className="page-topbar">
        <button className="icon-button" type="button" aria-label="Back to main page" onClick={onBack}>
          <ChevronLeft size={22} aria-hidden="true" />
        </button>
        <h2>{title}</h2>
        <span aria-hidden="true"></span>
      </header>

      <div className="language-content">
        <p className="language-heading">{heading}</p>
        <div className="language-bubbles">
          {languages.map((item) => (
            <button
              className={`language-bubble ${language === item ? 'selected' : ''}`}
              key={item}
              type="button"
              onClick={() => onLanguage(item)}
            >
              {item}
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}
