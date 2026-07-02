import React from 'react';
import { BookOpen, CalendarDays } from 'lucide-react';

export function BottomNav({ activeScreen, onNavigate, t }) {
  return (
    <nav className={`bottom-bar active-${activeScreen}`} aria-label="Main navigation">
      <span className="bottom-nav-indicator" aria-hidden="true" />
      <button
        className={`tab-button ${activeScreen === 'learn' ? 'active' : ''}`}
        data-intro-target="start-learning"
        type="button"
        onClick={() => onNavigate('learn')}
      >
        <BookOpen size={21} aria-hidden="true" />
        <span>{t.startLearning}</span>
      </button>
      <button
        className={`tab-button ${activeScreen === 'main' ? 'active' : ''}`}
        data-intro-target="overview"
        type="button"
        onClick={() => onNavigate('main')}
      >
        <CalendarDays size={21} aria-hidden="true" />
        <span>{t.overview}</span>
      </button>
    </nav>
  );
}
