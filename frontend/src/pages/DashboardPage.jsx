import React from 'react';
import { CheckCircle2, Flame, Settings, Target } from 'lucide-react';
import { BottomNav } from '../components/BottomNav';

export function DashboardPage({
  activePlan,
  recentFinishedLessons,
  recentLessons,
  weeklyPlan,
  onOpenLesson,
  onOpenPlan,
  onOpenSettings,
  onNavigate,
}) {
  return (
    <section className="main-page" aria-label="Overview dashboard">
      <header className="main-topbar">
        <div>
          <p className="app-label">Welcome back</p>
          <h2>Dashboard</h2>
        </div>
        <button className="icon-button" type="button" aria-label="Settings" onClick={onOpenSettings}>
          <Settings size={22} aria-hidden="true" />
        </button>
      </header>

      <div className="dashboard-content">
        <button
          className={`dashboard-hero dashboard-hero-button colorful-card ${activePlan.accent}`}
          type="button"
          aria-label="Open current learning plan"
          onClick={onOpenPlan}
        >
          <div>
            <p>{activePlan.language} plan</p>
            <h3>{activePlan.title}</h3>
            <span>{activePlan.lessons.length} lessons in progress</span>
          </div>
          <div className="hero-progress" aria-label={`Overall progress ${activePlan.progress} percent`}>
            <strong>{activePlan.progress}%</strong>
            <small>Overall</small>
          </div>
        </button>

        <section className="week-card" aria-label="Weekly training plan">
          <div className="section-heading">
            <h3>This week</h3>
            <span>3 day streak</span>
          </div>
          <div className="week-row">
            {weeklyPlan.map((item, index) => (
              <span className={`week-day ${item.done ? 'done' : ''}`} key={`${item.day}-${index}`}>
                {item.day}
              </span>
            ))}
          </div>
        </section>

        <div className="overview-grid" aria-label="Learning stats">
          <section className="stat-card">
            <CheckCircle2 size={20} aria-hidden="true" />
            <strong>6/15</strong>
            <span>Exercises done</span>
          </section>
          <section className="stat-card">
            <Target size={20} aria-hidden="true" />
            <strong>78%</strong>
            <span>Average score</span>
          </section>
          <section className="stat-card">
            <Flame size={20} aria-hidden="true" />
            <strong>Vocabulary</strong>
            <span>Weak area</span>
          </section>
        </div>

        <LessonListBlock
          emptyText="No ongoing lessons yet"
          lessons={recentLessons}
          onOpenLesson={onOpenLesson}
          title="Recent progress"
        />

        <LessonListBlock
          emptyText="No finished lessons yet"
          lessons={recentFinishedLessons}
          onOpenLesson={onOpenLesson}
          title="Recent finish"
        />
      </div>

      <BottomNav activeScreen="main" onNavigate={onNavigate} />
    </section>
  );
}

function LessonListBlock({ emptyText, lessons, onOpenLesson, title }) {
  return (
    <section className="lesson-progress-card" aria-label={title}>
      <div className="section-heading">
        <h3>{title}</h3>
        <span>Top 3 lessons</span>
      </div>
      {lessons.length === 0 ? (
        <p className="empty-state">{emptyText}</p>
      ) : (
        lessons.map((lesson, index) => (
          <button
            className="progress-row progress-link"
            key={`${lesson.planTitle}-${lesson.title}`}
            type="button"
            onClick={() => onOpenLesson(lesson.planIndex, lesson.lessonIndex)}
          >
            <span className={`mini-dot ${lesson.accent}`}>{index + 1}</span>
            <div>
              <strong>{lesson.title}</strong>
              <em>{lesson.planTitle}</em>
              <div className="progress-track" aria-hidden="true">
                <span style={{ width: `${lesson.progress}%` }}></span>
              </div>
            </div>
            <small>{lesson.progress}%</small>
          </button>
        ))
      )}
    </section>
  );
}
