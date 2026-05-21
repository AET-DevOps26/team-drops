import React from 'react';
import { BookOpen, CalendarDays, Clock3 } from 'lucide-react';

export function BottomNav({ activeScreen, onNavigate }) {
  return (
    <nav className="bottom-bar" aria-label="Main navigation">
      <button
        className={`tab-button ${activeScreen === 'learn' ? 'active' : ''}`}
        type="button"
        onClick={() => onNavigate('learn')}
      >
        <BookOpen size={21} aria-hidden="true" />
        <span>Start learning</span>
      </button>
      <button
        className={`tab-button ${activeScreen === 'main' ? 'active' : ''}`}
        type="button"
        onClick={() => onNavigate('main')}
      >
        <CalendarDays size={21} aria-hidden="true" />
        <span>Overview</span>
      </button>
      <button className="tab-button" type="button">
        <Clock3 size={21} aria-hidden="true" />
        <span>History</span>
      </button>
    </nav>
  );
}
