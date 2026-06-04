import React from 'react';
import {
  BookOpen,
  BrainCircuit,
  CalendarRange,
  CheckCircle2,
  ChevronLeft,
  Clock3,
  Headphones,
  ListChecks,
  Mic,
  PenLine,
  Sparkles,
  Settings,
  Target,
  TextCursorInput,
} from 'lucide-react';
import { BottomNav } from '../components/BottomNav';

export function LearningPage({
  activeExercise,
  activeLesson,
  activePlan,
  answerError,
  answerPending,
  learningError,
  learningMode,
  learningPlans,
  learningStep,
  t,
  onBack,
  onLearningMode,
  onNavigate,
  onOpenExercise,
  onOpenPlan,
  onOpenPlanDetail,
  onOpenSettings,
  onSubmitAnswer,
  onOpenLesson,
}) {
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
            {learningStep === 'exercise'
              ? t.exercise
              : learningStep === 'lesson'
              ? t.lesson
              : learningStep === 'planDetail'
                ? t.planOverview
              : learningStep === 'lessons'
                  ? t.learningPlan
                  : t.learn}
          </h2>
        </div>
        <button className="icon-button" type="button" aria-label="Settings" onClick={onOpenSettings}>
          <Settings size={22} aria-hidden="true" />
        </button>
      </header>

      <div className="learning-content">
        {learningError && <p className="auth-error" role="alert">{learningError}</p>}

        {learningStep === 'plans' && (
          <div className="learning-options" aria-label="Learning type">
            <button
              className={`mode-card ${learningMode === 'training' ? 'selected' : ''}`}
              type="button"
              onClick={() => onLearningMode('training')}
            >
              <BookOpen size={22} aria-hidden="true" />
              <span>
                <strong>{t.training}</strong>
                <small>{t.trainingDescription}</small>
              </span>
            </button>
            <button
              className={`mode-card ${learningMode === 'ai' ? 'selected' : ''}`}
              type="button"
              onClick={() => onLearningMode('ai')}
            >
              <BrainCircuit size={22} aria-hidden="true" />
              <span>
                <strong>{t.aiTraining}</strong>
                <small>{t.comingLater}</small>
              </span>
            </button>
          </div>
        )}

        {learningMode === 'training' ? (
          <>
            {learningStep === 'plans' && <LearningPlansView learningPlans={learningPlans} t={t} onOpenPlan={onOpenPlan} />}
            {learningStep === 'lessons' && (
              <LessonsView
                activePlan={activePlan}
                onOpenLesson={onOpenLesson}
                onOpenPlanDetail={onOpenPlanDetail}
                t={t}
              />
            )}
            {learningStep === 'planDetail' && <PlanDetailView activePlan={activePlan} />}
            {learningStep === 'lesson' && (
              <LessonDetailView activeLesson={activeLesson} t={t} onOpenExercise={onOpenExercise} />
            )}
            {learningStep === 'exercise' && (
              <ExerciseDetailView
                activeExercise={activeExercise}
                answerError={answerError}
                answerPending={answerPending}
                onSubmitAnswer={onSubmitAnswer}
              />
            )}
          </>
        ) : (
          <AiLearningPlanForm t={t} />
        )}
      </div>

      <BottomNav activeScreen="learn" t={t} onNavigate={onNavigate} />
    </section>
  );
}

const exerciseTypeOptions = [
  { id: 'reading', label: 'Reading' },
  { id: 'listening', label: 'Listening' },
  { id: 'writing', label: 'Writing' },
  { id: 'speaking', label: 'Speaking' },
];

function AiLearningPlanForm({ t }) {
  const [selectedExerciseTypes, setSelectedExerciseTypes] = React.useState(
    exerciseTypeOptions.filter((type) => !isComingSoonType(type.id)).map((type) => type.id),
  );
  const aiUnavailableMessage = 'AI plan generation needs the backend learning service and GenAI integration to be available.';

  const toggleExerciseType = (exerciseType) => {
    if (isComingSoonType(exerciseType)) {
      window.alert(t.comingSoon);
      return;
    }

    setSelectedExerciseTypes((currentTypes) =>
      currentTypes.includes(exerciseType)
        ? currentTypes.filter((type) => type !== exerciseType)
        : [...currentTypes, exerciseType],
    );
  };

  return (
    <form className="ai-plan-form" aria-label="AI learning plan generator">
      <section className="ai-form-hero">
        <span className="ai-form-icon">
          <BrainCircuit size={24} aria-hidden="true" />
        </span>
        <div>
          <p>AI Training</p>
          <h3>Generate learning plan</h3>
        </div>
      </section>

      <p className="auth-error ai-plan-notice" role="status">{aiUnavailableMessage}</p>

      <label className="ai-form-field ai-form-field-full">
        <span>Main learning plan description / goal</span>
        <textarea
          name="learningGoal"
          placeholder="Example: Prepare for a German job interview with technical project questions."
          rows={4}
        />
      </label>

      <div className="ai-form-grid">
        <label className="ai-form-field">
          <span>
            <CalendarRange size={15} aria-hidden="true" />
            Duration
          </span>
          <input inputMode="numeric" min="1" name="durationWeeks" placeholder="4" type="number" />
          <small>weeks</small>
        </label>

        <label className="ai-form-field">
          <span>
            <Clock3 size={15} aria-hidden="true" />
            Study time
          </span>
          <input inputMode="numeric" min="1" name="studyHoursPerWeek" placeholder="5" type="number" />
          <small>hrs / week</small>
        </label>

        <label className="ai-form-field">
          <span>
            <ListChecks size={15} aria-hidden="true" />
            Maximum lessons
          </span>
          <input inputMode="numeric" min="1" name="maximumLessons" placeholder="12" type="number" />
        </label>

        <label className="ai-form-field">
          <span>
            <ListChecks size={15} aria-hidden="true" />
            Minimum lessons
          </span>
          <input inputMode="numeric" min="1" name="minimumLessons" placeholder="6" type="number" />
        </label>
      </div>

      <fieldset className="ai-exercise-types">
        <legend>Exercise types</legend>
        <div className="ai-checkbox-grid">
          {exerciseTypeOptions.map((type) => (
            <label className={`ai-checkbox-option ${isComingSoonType(type.id) ? 'is-disabled' : ''}`} key={type.id}>
              <input
                checked={selectedExerciseTypes.includes(type.id)}
                name="exerciseTypes"
                onChange={() => toggleExerciseType(type.id)}
                type="checkbox"
                value={type.id}
              />
              <span className={`exercise-type-icon ${type.id}`}>
                <ExerciseTypeIcon type={type.id} />
              </span>
              <strong>{type.label}</strong>
            </label>
          ))}
        </div>
      </fieldset>

      <button className="ai-generate-button" disabled type="button">
        <Sparkles size={18} aria-hidden="true" />
        Backend required
      </button>
    </form>
  );
}

function LearningPlansView({ learningPlans, t, onOpenPlan }) {
  if (learningPlans.length === 0) {
    return (
      <section className="plan-section" aria-label="Suggested learning plans">
        <div className="section-heading">
          <h3>{t.suggestedPlans}</h3>
          <span>0 {t.plans}</span>
        </div>
        <p className="empty-state no-plan-empty-state">Current no learning plan.</p>
      </section>
    );
  }

  return (
    <section className="plan-section" aria-label="Suggested learning plans">
      <div className="section-heading">
        <h3>{t.suggestedPlans}</h3>
        <span>{learningPlans.length} {t.plans}</span>
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
                <small>{t.overall}</small>
              </span>
            </button>
          );
        })}
      </div>
    </section>
  );
}

function LessonsView({ activePlan, onOpenLesson, onOpenPlanDetail }) {
  const PlanIcon = activePlan.lessons[0]?.icon ?? BookOpen;

  if (activePlan.lessons.length === 0) {
    return (
      <section className="summary-card">
        <div className="section-heading">
          <h3>No lessons yet</h3>
          <span>{activePlan.language}</span>
        </div>
        <p>This plan exists in the backend, but it does not contain any lessons yet.</p>
      </section>
    );
  }

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

function LessonDetailView({ activeLesson, t, onOpenExercise }) {
  const LessonIcon = activeLesson.icon ?? BookOpen;
  const finished = activeLesson.status === 'finished';

  return (
    <div className="lesson-detail-stack" aria-label="Lesson detail">
      <section className={`lesson-overview colorful-plan-header ${activeLesson.accent}`}>
        <div className={`lesson-icon ${activeLesson.accent}`}>
          <LessonIcon size={24} aria-hidden="true" />
        </div>
        <div className="lesson-summary">
          <p>{activeLesson.status}</p>
          <h3>{activeLesson.title}</h3>
          <span>{activeLesson.topic}</span>
        </div>
      </section>

      {activeLesson.blocks.map((block, index) => (
        <LessonBlock
          block={block}
          exercises={activeLesson.exercises}
          key={`${block.type}-${block.title ?? block.exerciseIndex}-${index}`}
          t={t}
          onOpenExercise={onOpenExercise}
        />
      ))}

      {finished && (
        <section className="completion-comment">
          <CheckCircle2 size={20} aria-hidden="true" />
          <p>{activeLesson.comment}</p>
        </section>
      )}
    </div>
  );
}

function LessonBlock({ block, exercises, t, onOpenExercise }) {
  if (block.type === 'exercise') {
    const exercise = exercises[block.exerciseIndex];

    return (
      <section className="lesson-block-group">
        <div className="section-heading">
          <h3>{exercise.title}</h3>
          <span>{exercise.type}</span>
        </div>
        <ExerciseCard exercise={exercise} index={block.exerciseIndex} t={t} onOpenExercise={onOpenExercise} />
      </section>
    );
  }

  if (block.type === 'summary') {
    return (
      <section className="summary-card">
        <div className="section-heading">
          <h3>{block.title}</h3>
          <span>Wrap-up</span>
        </div>
        <p>{block.text}</p>
      </section>
    );
  }

  return (
    <section className="summary-card">
      <div className="section-heading">
        <h3>{block.title}</h3>
        <span>{block.subtitle}</span>
      </div>
      <p>{block.text}</p>

      {block.visual && (
        <div className="lesson-visual" aria-label={block.visual.label}>
          {block.visual.steps.map((step) => (
            <span key={step}>{step}</span>
          ))}
        </div>
      )}

      <div className="learning-point-map" aria-label={`${block.title} points`}>
        {block.points.map((point, index) => (
          <div className="learning-point" key={point}>
            <span>{index + 1}</span>
            <p>{point}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function ExerciseCard({ exercise, index, t, onOpenExercise }) {
  const comingSoon = isComingSoonType(exercise.type);

  return (
    <button
      className={`exercise-card lesson-progress-link exercise-type-${exercise.type} ${comingSoon ? 'is-disabled' : ''}`}
      type="button"
      onClick={() => (comingSoon ? window.alert(t.comingSoon) : onOpenExercise(index))}
    >
      <span className={`exercise-type-icon ${exercise.type}`}>
        <ExerciseTypeIcon type={exercise.type} />
      </span>
      <span className="lesson-progress-copy">
        <strong>{exercise.title}</strong>
        <em>{exercise.format}</em>
        <span className="status-pill">{formatStatus(exercise.status)}</span>
      </span>
      <small className="lesson-percent">{exercise.grade ? `${exercise.grade}` : '--'}</small>
    </button>
  );
}

function isComingSoonType(type) {
  return type === 'listening' || type === 'speaking';
}

function ExerciseDetailView({ activeExercise, answerError, answerPending, onSubmitAnswer }) {
  const [answerText, setAnswerText] = React.useState(activeExercise.answerText ?? '');

  React.useEffect(() => {
    setAnswerText(activeExercise.answerText ?? '');
  }, [activeExercise.id, activeExercise.answerText]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    await onSubmitAnswer(activeExercise, answerText);
  };

  if (!activeExercise.id) {
    return (
      <section className="summary-card">
        <div className="section-heading">
          <h3>No exercise selected</h3>
          <span>Practice</span>
        </div>
        <p>Open an exercise from the lesson detail screen to load it from the backend.</p>
      </section>
    );
  }

  return (
    <section className="exercise-detail" aria-label="Exercise detail">
      <div className="exercise-detail-header">
        <span className={`exercise-type-icon ${activeExercise.type}`}>
          <ExerciseTypeIcon type={activeExercise.type} />
        </span>
        <div>
          <p>{activeExercise.type}</p>
          <h3>{activeExercise.title}</h3>
        </div>
      </div>

      <div className="exercise-status-row">
        <span className="status-pill">{formatStatus(activeExercise.status)}</span>
        {activeExercise.status === 'finished' && (
          <strong className="grade-pill">Grade {activeExercise.grade}/100</strong>
        )}
      </div>

      <section className="summary-card">
        <div className="section-heading">
          <h3>Task</h3>
          <span>{activeExercise.format}</span>
        </div>
        <p>{activeExercise.task}</p>
      </section>

      <section className="summary-card">
        <div className="section-heading">
          <h3>Expected answer</h3>
          <span>{activeExercise.type}</span>
        </div>
        <p>{activeExercise.expectedAnswer ?? 'Open-ended exercise.'}</p>
      </section>

      <form className="summary-card" onSubmit={handleSubmit}>
        <div className="section-heading">
          <h3>Your answer</h3>
          <span>Submit to backend</span>
        </div>
        <textarea
          className="auth-field"
          name="answerText"
          rows={5}
          value={answerText}
          onChange={(event) => setAnswerText(event.target.value)}
        />
        {answerError && <p className="auth-error" role="alert">{answerError}</p>}
        <button className="auth-button register-button" disabled={answerPending} type="submit">
          {answerPending ? 'Submitting...' : 'Submit answer'}
        </button>
      </form>

      {activeExercise.feedback && <ExerciseFeedback feedback={activeExercise.feedback} />}
    </section>
  );
}

function ExerciseFeedback({ feedback }) {
  return (
    <section className="feedback-card" aria-label="Exercise feedback">
      <div className="feedback-score">
        <CheckCircle2 size={20} aria-hidden="true" />
        <div>
          <p>{feedback.title}</p>
          <strong>{feedback.score}/100</strong>
        </div>
      </div>
      <p>{feedback.message}</p>

      {(feedback.strengths.length > 0 || feedback.improvements.length > 0) && (
        <div className="feedback-grid">
          {feedback.strengths.length > 0 && (
            <div>
              <h4>Strengths</h4>
              {feedback.strengths.map((item) => (
                <span key={item}>{item}</span>
              ))}
            </div>
          )}
          {feedback.improvements.length > 0 && (
            <div>
              <h4>Improve</h4>
              {feedback.improvements.map((item) => (
                <span key={item}>{item}</span>
              ))}
            </div>
          )}
        </div>
      )}

      {feedback.improvedExample && (
        <div className="feedback-example">
          <h4>Improved example</h4>
          <p>{feedback.improvedExample}</p>
        </div>
      )}
    </section>
  );
}

function ExerciseTypeIcon({ type }) {
  if (type === 'listening') {
    return <Headphones size={18} aria-hidden="true" />;
  }

  if (type === 'writing') {
    return <PenLine size={18} aria-hidden="true" />;
  }

  if (type === 'speaking') {
    return <Mic size={18} aria-hidden="true" />;
  }

  return <TextCursorInput size={18} aria-hidden="true" />;
}

function formatStatus(status) {
  if (status === 'not-started') {
    return 'Not yet start';
  }

  if (status === 'ongoing') {
    return 'Ongoing';
  }

  return 'Finished';
}
