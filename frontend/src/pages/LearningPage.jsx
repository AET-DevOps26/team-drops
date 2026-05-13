import React from 'react';
import { BookOpen, BrainCircuit, CheckCircle2, ChevronLeft, PenLine, Settings, Target } from 'lucide-react';
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
        <div className="topbar-title">
          {learningMode === 'training' && learningStep !== 'plans' && (
            <button className="inline-back" type="button" aria-label="Back" onClick={onBack}>
              <ChevronLeft size={18} aria-hidden="true" />
            </button>
          )}
          <h2>
            {learningStep === 'lesson'
              ? activeLesson.title
              : learningStep === 'planDetail'
                ? 'Plan overview'
                : learningStep === 'lessons'
                  ? activePlan.title
                  : 'Learning Hub'}
          </h2>
        </div>
        <button className="icon-button" type="button" aria-label="Settings" onClick={onOpenSettings}>
          <Settings size={22} aria-hidden="true" />
        </button>
      </header>

      <div className="learning-content">
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
          const firstLesson = plan.lessons[0];
          const PlanIcon = firstLesson.icon;

          return (
            <button className="plan-card" key={plan.title} type="button" onClick={() => onOpenPlan(planIndex)}>
              <span className={`module-icon ${firstLesson.accent}`}>
                <PlanIcon size={19} aria-hidden="true" />
              </span>
              <span className="module-copy">
                <strong>{plan.title}</strong>
                <small>{plan.description}</small>
              </span>
              <span className="lesson-percent">{plan.progress}%</span>
            </button>
          );
        })}
      </div>
    </section>
  );
}

function LessonsView({ activeLesson, activePlan, onOpenLesson, onOpenPlanDetail }) {
  const ActiveLessonIcon = activeLesson.icon;

  return (
    <>
      <button
        className="lesson-overview plan-overview-button"
        type="button"
        aria-label="Open learning plan overview"
        onClick={onOpenPlanDetail}
      >
        <div className={`lesson-icon ${activeLesson.accent}`}>
          <ActiveLessonIcon size={24} aria-hidden="true" />
        </div>
        <div className="lesson-summary">
          <p>{activePlan.duration}</p>
          <h3>{activePlan.title}</h3>
          <span>{activePlan.goal}</span>
        </div>
      </button>

      <div className="exercise-list" aria-label="Lessons in selected plan">
        {activePlan.lessons.map((lesson, index) => {
          const LessonIcon = lesson.icon;
          return (
            <button className="exercise-card" key={lesson.title} type="button" onClick={() => onOpenLesson(index)}>
              <span className={`module-icon ${lesson.accent}`}>
                <LessonIcon size={19} aria-hidden="true" />
              </span>
              <span className="module-copy">
                <strong>{lesson.title}</strong>
                <small>{lesson.topic}</small>
              </span>
              <span className="lesson-percent">{lesson.progress}%</span>
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
      <section className="dashboard-hero compact-hero">
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

      <div className="overview-grid" aria-label="Learning plan stats">
        <section className="stat-card">
          <Target size={20} aria-hidden="true" />
          <strong>{activePlan.goal}</strong>
          <span>Learning target</span>
        </section>
        <section className="stat-card">
          <BookOpen size={20} aria-hidden="true" />
          <strong>{activePlan.lessons.length}</strong>
          <span>Lessons</span>
        </section>
        <section className="stat-card">
          <CheckCircle2 size={20} aria-hidden="true" />
          <strong>{activePlan.progress}%</strong>
          <span>Progress</span>
        </section>
      </div>

      <section className="summary-card">
        <div className="section-heading">
          <h3>Learning summary</h3>
          <span>{activePlan.language}</span>
        </div>
        <p>{activePlan.summary}</p>
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
