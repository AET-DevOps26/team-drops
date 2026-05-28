import React from 'react';
import { CheckCircle2, Settings, Target } from 'lucide-react';
import { BottomNav } from '../components/BottomNav';

export function DashboardPage({
  activePlan,
  profile,
  recentFinishedLessons,
  recentLessons,
  t,
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
          <p className="app-label">{t.welcomeBack}, {profile.name}</p>
          <h2>{t.dashboard}</h2>
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
            <p>{activePlan.language} {t.currentPlan}</p>
            <h3>{activePlan.title}</h3>
            <span>{activePlan.lessons.length} {t.lessonsInProgress}</span>
          </div>
          <div className="hero-progress" aria-label={`Overall progress ${activePlan.progress} percent`}>
            <strong>{activePlan.progress}%</strong>
            <small>{t.overall}</small>
          </div>
        </button>

        <div className="overview-grid" aria-label="Learning stats">
          <section className="stat-card">
            <CheckCircle2 size={20} aria-hidden="true" />
            <strong>6/15</strong>
            <span>{t.exercisesDone}</span>
          </section>
          <section className="stat-card">
            <Target size={20} aria-hidden="true" />
            <strong>78%</strong>
            <span>{t.averageScore}</span>
          </section>
        </div>

        <LessonListBlock
          emptyText={t.noOngoing}
          lessons={recentLessons}
          onOpenLesson={onOpenLesson}
          t={t}
          title={t.recentProgress}
        />

        <LessonListBlock
          emptyText={t.noFinished}
          lessons={recentFinishedLessons}
          onOpenLesson={onOpenLesson}
          t={t}
          title={t.recentFinish}
        />
      </div>

      <BottomNav activeScreen="main" t={t} onNavigate={onNavigate} />
    </section>
  );
}

function LessonListBlock({ emptyText, lessons, onOpenLesson, t, title }) {
  return (
    <section className="lesson-progress-card" aria-label={title}>
      <div className="section-heading">
        <h3>{title}</h3>
        <span>{t.topLessons}</span>
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
