import React from 'react';
import {
  getRagTopics,
  queryRag,
} from '../api/client';
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
            <button
              className={`mode-card ${learningMode === 'rag' ? 'selected' : ''}`}
              type="button"
              onClick={() => onLearningMode('rag')}
            >
              <Target size={22} aria-hidden="true" />
              <span>
                <strong>{t.ragLearning}</strong>
                <small>{t.ragLearningDescription}</small>
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
        ) : learningMode === 'rag' ? (
          <RagLearningFlow t={t} />
        ) : (
          <AiTrainingPlaceholder t={t} />
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

const ragTopics = [
  {
    id: 'job interview',
    title: 'Job interview',
    description: 'Practice answers, interview etiquette, follow-up, and preparation.',
  },
  {
    id: 'Reisen in der Schweiz',
    title: 'Reisen in der Schweiz',
    description: 'Routen, Regionen, Wandern, Unterkünfte und Reiseideen in der Schweiz.',
  },
];

const ragTopicDescriptions = {
  'job interview': 'Practice answers, interview etiquette, follow-up, and preparation.',
  'Reisen in der Schweiz': 'Routen, Regionen, Wandern, Unterkunfte und Reiseideen in der Schweiz.',
};

function toRagTopic(topicName) {
  return {
    id: topicName,
    title: topicName
      .split(' ')
      .map((word) => (word.length > 0 ? `${word[0].toUpperCase()}${word.slice(1)}` : word))
      .join(' '),
    description: ragTopicDescriptions[topicName] ?? 'Ask questions grounded in this document topic.',
  };
}

function AiTrainingPlaceholder({ t }) {
  return (
    <section className="ai-plan-form" aria-label="AI training">
      <section className="ai-form-hero">
        <span className="ai-form-icon">
          <BrainCircuit size={24} aria-hidden="true" />
        </span>
        <div>
          <p>{t.aiTraining}</p>
          <h3>{t.comingSoon}</h3>
        </div>
      </section>
      <p className="empty-state no-plan-empty-state">{t.aiTrainingDescription}</p>
    </section>
  );
}

function RagLearningFlow({ t }) {
  const [selectedTopic, setSelectedTopic] = React.useState(null);
  const [topics, setTopics] = React.useState(ragTopics);
  const [topicsPending, setTopicsPending] = React.useState(false);
  const [topicsError, setTopicsError] = React.useState('');

  React.useEffect(() => {
    let cancelled = false;

    async function loadTopics() {
      setTopicsPending(true);
      setTopicsError('');

      try {
        const response = await getRagTopics();
        if (cancelled) {
          return;
        }

        const nextTopics = (response?.topics ?? []).map(toRagTopic);
        setTopics(nextTopics.length > 0 ? nextTopics : ragTopics);
      } catch (error) {
        if (!cancelled) {
          setTopicsError(error.message || 'Unable to load RAG topics.');
          setTopics(ragTopics);
        }
      } finally {
        if (!cancelled) {
          setTopicsPending(false);
        }
      }
    }

    loadTopics();

    return () => {
      cancelled = true;
    };
  }, []);

  if (!selectedTopic) {
    return (
      <RagTopicPicker
        pending={topicsPending}
        t={t}
        topics={topics}
        topicsError={topicsError}
        onSelectTopic={setSelectedTopic}
      />
    );
  }

  return (
    <RagQuestionForm
      selectedTopic={selectedTopic}
      t={t}
      onBackToTopics={() => setSelectedTopic(null)}
    />
  );
}

function RagTopicPicker({ pending, t, topics, topicsError, onSelectTopic }) {
  return (
    <section className="rag-topic-section" aria-label="RAG learning topics">
      <section className="ai-form-hero">
        <span className="ai-form-icon">
          <Target size={24} aria-hidden="true" />
        </span>
        <div>
          <p>{t.ragLearning}</p>
          <h3>{t.chooseRagTopic}</h3>
        </div>
      </section>

      {topicsError && <p className="auth-error ai-plan-notice" role="status">{topicsError}</p>}
      {pending && <p className="empty-state no-plan-empty-state">Loading document topics...</p>}

      <div className="rag-topic-list">
        {topics.map((topic) => (
          <button
            className="rag-topic-card"
            key={topic.id}
            type="button"
            onClick={() => onSelectTopic(topic)}
          >
            <span className="rag-topic-icon">
              <Target size={19} aria-hidden="true" />
            </span>
            <span>
              <strong>{topic.title}</strong>
              <small>{topic.description}</small>
            </span>
          </button>
        ))}
      </div>
    </section>
  );
}

function RagQuestionForm({ selectedTopic, t, onBackToTopics }) {
  const [question, setQuestion] = React.useState('');
  const [topK, setTopK] = React.useState('4');
  const [answer, setAnswer] = React.useState(null);
  const [pending, setPending] = React.useState(false);
  const [error, setError] = React.useState('');

  const askQuestion = async (event) => {
    event.preventDefault();

    if (!question.trim()) {
      setError('Add a question before asking the RAG corpus.');
      return;
    }

    setPending(true);
    setError('');
    setAnswer(null);
    const sourceCount = Math.max(1, Math.min(8, Number(topK) || 4));

    try {
      const result = await queryRag({
        topic: selectedTopic.id,
        question: question.trim(),
        top_k: sourceCount,
        rebuild_corpus: false,
      });
      setAnswer(result);
    } catch (requestError) {
      setError(requestError.message || 'Unable to query this RAG topic.');
    } finally {
      setPending(false);
    }
  };

  return (
    <form className="ai-plan-form" aria-label="RAG question answering" onSubmit={askQuestion}>
      <section className="ai-form-hero">
        <span className="ai-form-icon">
          <Target size={24} aria-hidden="true" />
        </span>
        <div>
          <p>{t.ragLearning}</p>
          <h3>{selectedTopic.title}</h3>
        </div>
      </section>

      <button className="rag-topic-back" type="button" onClick={onBackToTopics}>
        <ChevronLeft size={16} aria-hidden="true" />
        {selectedTopic.title}
      </button>

      <label className="ai-form-field ai-form-field-full">
        <span>Question</span>
        <textarea
          name="ragQuestion"
          onChange={(event) => setQuestion(event.target.value)}
          placeholder="Example: What can I do in Basel-Land?"
          rows={4}
          value={question}
        />
      </label>

      <label className="ai-form-field">
        <span>
          <ListChecks size={15} aria-hidden="true" />
          Sources
        </span>
        <input
          inputMode="numeric"
          max="8"
          min="1"
          name="topK"
          onChange={(event) => setTopK(event.target.value)}
          type="number"
          value={topK}
        />
        <small>retrieved chunks</small>
      </label>

      {error && <p className="auth-error ai-plan-notice" role="alert">{error}</p>}

      <button className="ai-generate-button" disabled={pending} type="submit">
        <Sparkles size={18} aria-hidden="true" />
        {pending ? 'Asking RAG...' : 'Ask RAG'}
      </button>

      {answer && (
        <section className="rag-answer-card" aria-label="RAG answer">
          <h4>Answer</h4>
          <p>{answer.answer}</p>
          {answer.sources?.length > 0 && (
            <div className="rag-source-list">
              {answer.sources.map((source) => (
                <details className="rag-source-item" key={`${source.source}-${source.page}-${source.chunk_index}`}>
                  <summary>
                    {source.source}, page {source.page}
                  </summary>
                  <p>{source.text}</p>
                </details>
              ))}
            </div>
          )}
        </section>
      )}
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
