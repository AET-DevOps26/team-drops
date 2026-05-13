import React from 'react';
import { BookOpen, BrainCircuit, ChevronLeft, PenLine, Settings, Target } from 'lucide-react';
import { BottomNav } from '../components/BottomNav';

export function LearningPage({
  activeLesson,
  activePlan,
  learningMode,
  learningPlans,
  learningStep,
  onBack,
  onLearningMode,
  onNavigate,
  onOpenPlan,
  onOpenPlanDetail,
  onOpenSettings,
  onOpenLesson,
}) {
  const ActiveLessonIcon = activeLesson.icon;

  return (
    <section className="main-page" aria-label="Start learning">
      <header className="main-topbar">
        <div className={`topbar-title ${learningStep === 'lessons' ? 'learning-plan-title' : ''}`}>
          {learningMode === 'training' && learningStep !== 'plans' && (
            <button className="inline-back" type="button" aria-label="Back" onClick={onBack}>
              <ChevronLeft size={18} aria-hidden="true" />
            </button>
          )}
          <h2>
            {learningStep === 'lesson'
              ? 'Lesson'
              : learningStep === 'planDetail'
                ? 'Plan overview'
              : learningStep === 'lessons'
                  ? 'Learning Plan'
                  : 'Learning Hub'}
          </h2>
        </div>
        <button className="icon-button" type="button" aria-label="Settings" onClick={onOpenSettings}>
          <Settings size={22} aria-hidden="true" />
        </button>
      </header>

      <div className="learning-content">
        {learningStep === 'plans' && (
          <div className="learning-options" aria-label="Learning type">
            <button
              className={`mode-card ${learningMode === 'training' ? 'selected' : ''}`}
              type="button"
              onClick={() => onLearningMode('training')}
            >
              <BookOpen size={22} aria-hidden="true" />
              <span>
                <strong>Training</strong>
                <small>Guided interview lessons</small>
              </span>
            </button>
            <button
              className={`mode-card ${learningMode === 'ai' ? 'selected' : ''}`}
              type="button"
              onClick={() => onLearningMode('ai')}
            >
              <BrainCircuit size={22} aria-hidden="true" />
              <span>
                <strong>AI Training</strong>
                <small>Coming later</small>
              </span>
            </button>
          </div>
        )}

        {learningMode === 'training' ? (
          <>
            {learningStep === 'plans' && <LearningPlansView learningPlans={learningPlans} onOpenPlan={onOpenPlan} />}
            {learningStep === 'lessons' && (
              <LessonsView
                activeLesson={activeLesson}
                activePlan={activePlan}
                onOpenLesson={onOpenLesson}
                onOpenPlanDetail={onOpenPlanDetail}
              />
            )}
            {learningStep === 'planDetail' && <PlanDetailView activePlan={activePlan} />}
            {learningStep === 'lesson' && <LessonDetailView activeLesson={activeLesson} />}
          </>
        ) : (
          <section className="ai-placeholder" aria-label="AI Training placeholder">
            <BrainCircuit size={34} aria-hidden="true" />
            <h3>AI Training</h3>
            <p>Personalized AI practice will live here later. For now, continue with Training lessons.</p>
          </section>
        )}
      </div>

      <BottomNav activeScreen="learn" onNavigate={onNavigate} />
    </section>
  );
}

function LearningPlansView({ learningPlans, onOpenPlan }) {
  return (
    <section className="plan-section" aria-label="Suggested learning plans">
      <div className="section-heading">
        <h3>Suggested learning plans</h3>
        <span>{learningPlans.length} plans</span>
      </div>

      <div className="plan-list">
        {learningPlans.map((plan, planIndex) => {
          return (
            <button
              className={`plan-card colorful-card ${plan.accent}`}
              key={plan.title}
              type="button"
              onClick={() => onOpenPlan(planIndex)}
            >
              <span className="colorful-copy">
                <small>{plan.duration}</small>
                <strong>{plan.title}</strong>
                <em>{plan.description}</em>
              </span>
              <span className="hero-progress compact-progress" aria-label={`${plan.progress} percent complete`}>
                <strong>{plan.progress}%</strong>
                <small>Overall</small>
              </span>
            </button>
          );
        })}
      </div>
    </section>
  );
}

function LessonsView({ activeLesson, activePlan, onOpenLesson, onOpenPlanDetail }) {
  const PlanIcon = activePlan.lessons[0].icon;

  return (
    <>
      <button
        className={`lesson-overview plan-overview-button colorful-plan-header ${activePlan.accent}`}
        type="button"
        aria-label="Open learning plan overview"
        onClick={onOpenPlanDetail}
      >
        <div className={`lesson-icon ${activePlan.accent}`}>
          <PlanIcon size={24} aria-hidden="true" />
        </div>
        <div className="lesson-summary">
          <p>{activePlan.duration}</p>
          <h3>{activePlan.title}</h3>
          <span>{activePlan.goal}</span>
        </div>
      </button>

      <div className="exercise-list" aria-label="Lessons in selected plan">
        {activePlan.lessons.map((lesson, index) => {
          return (
            <button
              className={`exercise-card lesson-progress-link accent-${lesson.accent}`}
              key={lesson.title}
              type="button"
              onClick={() => onOpenLesson(index)}
            >
              <span className={`mini-dot ${lesson.accent}`}>{index + 1}</span>
              <span className="lesson-progress-copy">
                <strong>{lesson.title}</strong>
                <em>{lesson.topic}</em>
                <span className="progress-track" aria-hidden="true">
                  <span style={{ width: `${lesson.progress}%` }}></span>
                </span>
              </span>
              <small className="lesson-percent">{lesson.progress}%</small>
            </button>
          );
        })}
      </div>
    </>
  );
}

function PlanDetailView({ activePlan }) {
  return (
    <section className="plan-detail" aria-label="Learning plan overview">
      <section className={`dashboard-hero colorful-card compact-hero ${activePlan.accent}`}>
        <div>
          <p>{activePlan.duration}</p>
          <h3>{activePlan.title}</h3>
          <span>{activePlan.goal}</span>
        </div>
        <div className="hero-progress" aria-label={`Overall progress ${activePlan.progress} percent`}>
          <strong>{activePlan.progress}%</strong>
          <small>Overall</small>
        </div>
      </section>

      <section className="summary-card">
        <div className="summary-line">
          <Target size={18} aria-hidden="true" />
          <p>
            Your learning target is to prepare for <strong>{activePlan.goal}</strong>.
          </p>
        </div>
      </section>

      <section className="summary-card">
        <div className="section-heading">
          <h3>Learning summary</h3>
          <span>{activePlan.language}</span>
        </div>
        <p>{activePlan.summary}</p>
      </section>

      <section className="summary-card">
        <div className="summary-line">
          <BookOpen size={18} aria-hidden="true" />
          <p>
            This plan contains <strong>{activePlan.lessons.length} lessons</strong> designed for{' '}
            <strong>{activePlan.duration}</strong> of practice.
          </p>
        </div>
      </section>
    </section>
  );
}

function LessonDetailView({ activeLesson }) {
  return (
    <section className="prompt-panel lesson-detail" aria-label="Lesson detail">
      <div className="prompt-heading">
        <PenLine size={18} aria-hidden="true" />
        <h3>{activeLesson.title}</h3>
      </div>

      <p className="lesson-topic">{activeLesson.topic}</p>
      <p className="lesson-text">{activeLesson.text}</p>

      <div className="component-list" aria-label="Lesson components">
        {activeLesson.components.map((component) => (
          <span key={component}>{component}</span>
        ))}
      </div>

      <ol className="prompt-list">
        {activeLesson.exercises.map((exercise) => (
          <li key={exercise}>{exercise}</li>
        ))}
      </ol>
    </section>
  );
}
