import React from 'react';
import { BookOpen, CalendarDays } from 'lucide-react';

export function BottomNav({ activeScreen, onNavigate, t }) {
  return (
    <nav className="bottom-bar" aria-label="Main navigation">
      <button
        className={`tab-button ${activeScreen === 'learn' ? 'active' : ''}`}
        type="button"
        onClick={() => onNavigate('learn')}
      >
        <BookOpen size={21} aria-hidden="true" />
        <span>{t.startLearning}</span>
      </button>
      <button
        className={`tab-button ${activeScreen === 'main' ? 'active' : ''}`}
        type="button"
        onClick={() => onNavigate('main')}
      >
        <CalendarDays size={21} aria-hidden="true" />
        <span>{t.overview}</span>
      </button>
    </nav>
  );
}
