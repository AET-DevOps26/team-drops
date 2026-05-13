import React from 'react';
import { createRoot } from 'react-dom/client';
import {
  BookOpen,
  BrainCircuit,
  BriefcaseBusiness,
  CalendarDays,
  CheckCircle2,
  ChevronLeft,
  Clock3,
  Flame,
  GraduationCap,
  Languages,
  LogOut,
  Mail,
  MapPinned,
  MessageSquareText,
  Moon,
  PenLine,
  Plane,
  Presentation,
  Settings,
  Sparkles,
  Sun,
  Target,
  User,
  UserPlus,
  X,
} from 'lucide-react';
import './styles.css';

function App() {
  const [screen, setScreen] = React.useState('auth');
  const [settingsOpen, setSettingsOpen] = React.useState(false);
  const [settingsClosing, setSettingsClosing] = React.useState(false);
  const [darkMode, setDarkMode] = React.useState(false);
  const [language, setLanguage] = React.useState('English');
  const [selectedPlan, setSelectedPlan] = React.useState(0);
  const [selectedLesson, setSelectedLesson] = React.useState(0);
  const [learningMode, setLearningMode] = React.useState('training');
  const [learningStep, setLearningStep] = React.useState('plans');

  const openSettings = () => {
    setSettingsClosing(false);
    setSettingsOpen(true);
  };

  const closeSettings = (nextScreen) => {
    setSettingsClosing(true);
    window.setTimeout(() => {
      setSettingsOpen(false);
      setSettingsClosing(false);

      if (nextScreen) {
        setScreen(nextScreen);
      }
    }, 220);
  };

  const goToMain = () => {
    setScreen('main');
    setSettingsOpen(false);
    setSettingsClosing(false);
  };
  const logOut = () => {
    closeSettings('auth');
  };
  const openProfile = () => {
    closeSettings('profile');
  };
  const openLanguage = () => {
    closeSettings('language');
  };

  const languages = ['English', 'German', 'Spanish', 'French', 'Italian', 'Japanese'];
  const interviewLessons = [
    {
      title: 'Self Introduction',
      topic: 'Introduce yourself professionally in an interview.',
      icon: MessageSquareText,
      accent: 'blue',
      time: '8 min',
      progress: 60,
      components: ['Text guide', 'Speaking exercise', 'Vocabulary upgrade'],
      text: 'A strong self-introduction is short, professional, and relevant. Start with who you are, mention your field or current role, then connect your experience to the position.',
      exercises: [
        'Tell me about yourself.',
        'Write a short professional introduction.',
        'Improve your introduction using more formal vocabulary.',
      ],
    },
    {
      title: 'Education and Background',
      topic: 'Explain your studies, university, and academic background.',
      icon: GraduationCap,
      accent: 'green',
      time: '7 min',
      progress: 20,
      components: ['Text guide', 'Writing exercise', 'Pronunciation practice'],
      text: 'When explaining your education, name your degree, university, specialization, and academic focus. Keep the answer clear and connect your studies to your career direction.',
      exercises: [
        'Describe your degree and specialization.',
        'Explain why you chose your field.',
        'Practice saying your graduation status clearly.',
      ],
    },
    {
      title: 'Work Experience and Internships',
      topic: 'Talk about previous internships, jobs, or projects.',
      icon: BriefcaseBusiness,
      accent: 'amber',
      time: '9 min',
      progress: 0,
      components: ['Text guide', 'Speaking exercise', 'Reflection'],
      text: 'Work experience answers should explain the context, your responsibilities, and the result. Use active verbs and mention one concrete thing you learned.',
      exercises: [
        'Describe one internship or work experience.',
        'Explain your responsibilities.',
        'Mention what you learned from the experience.',
      ],
    },
    {
      title: 'Project Explanation',
      topic: 'Present a technical or academic project clearly.',
      icon: Presentation,
      accent: 'violet',
      time: '10 min',
      progress: 0,
      components: ['Structure guide', 'Technical explanation', 'Simplification task'],
      text: 'A good project explanation follows a simple structure: problem, solution, your role, and impact. For non-technical listeners, avoid jargon and explain the value first.',
      exercises: [
        'Describe one project you worked on.',
        'Explain the problem, your solution, and your role.',
        'Simplify a technical explanation for a non-technical interviewer.',
      ],
    },
    {
      title: 'Strengths and Weaknesses',
      topic: 'Answer common HR questions about strengths and weaknesses.',
      icon: Sparkles,
      accent: 'rose',
      time: '8 min',
      progress: 0,
      components: ['Answer framework', 'Writing exercise', 'Improvement task'],
      text: 'Strengths need short examples. Weaknesses should sound honest but professional: name the weakness, explain what you are doing to improve, and avoid critical job requirements.',
      exercises: [
        'Name two strengths with examples.',
        'Explain one weakness professionally.',
        'Rewrite weak answers into stronger interview answers.',
      ],
    },
  ];
  const italyLessons = [
    {
      title: 'Italian Travel Basics',
      topic: 'Greet people, introduce yourself, and handle polite travel phrases.',
      icon: Languages,
      accent: 'green',
      time: '12 min',
      progress: 35,
      components: ['Phrase list', 'Speaking exercise', 'Mini quiz'],
      text: 'Start with high-frequency travel phrases. Practice greetings, your name, where you are from, and polite expressions you can reuse in hotels, cafes, and shops.',
      exercises: [
        'Introduce yourself as a traveler.',
        'Say where you are from and how long you will stay.',
        'Practice polite greetings for hotels and cafes.',
      ],
    },
    {
      title: 'Ordering Food',
      topic: 'Order meals, ask about ingredients, and pay at restaurants.',
      icon: MessageSquareText,
      accent: 'amber',
      time: '15 min',
      progress: 15,
      components: ['Menu vocabulary', 'Role-play exercise', 'Listening practice'],
      text: 'Ordering food is about polite requests and useful menu words. Practice asking for items, checking ingredients, and closing the interaction naturally.',
      exercises: [
        'Order a coffee and pastry.',
        'Ask for a vegetarian option.',
        'Request the bill politely.',
      ],
    },
    {
      title: 'Asking for Directions',
      topic: 'Find stations, hotels, landmarks, and basic transport routes.',
      icon: MapPinned,
      accent: 'blue',
      time: '14 min',
      progress: 0,
      components: ['Map phrases', 'Speaking exercise', 'Scenario practice'],
      text: 'Direction practice focuses on a small set of phrases: where something is, how to get there, and whether it is nearby. Learn the response words before long sentences.',
      exercises: [
        'Ask how to get to the train station.',
        'Understand left, right, straight ahead, and nearby.',
        'Explain that you are lost and need help.',
      ],
    },
    {
      title: 'Daily Travel Problems',
      topic: 'Handle simple problems at hotels, shops, and ticket counters.',
      icon: Plane,
      accent: 'violet',
      time: '13 min',
      progress: 0,
      components: ['Useful sentences', 'Problem-solving exercise', 'Review quiz'],
      text: 'Travel problems need calm, simple sentences. Practice saying what happened, what you need, and one clarifying question.',
      exercises: [
        'Say that your reservation is under your name.',
        'Ask for help when a train is delayed.',
        'Describe a missing item simply.',
      ],
    },
  ];
  const learningPlans = [
    {
      title: 'German Job Interview Preparation',
      description: 'Personalized plan to help you succeed in German job interviews.',
      summary:
        'Build confident answers for common HR and technical interview questions with short text guides, speaking practice, and answer improvement tasks.',
      language: 'German',
      goal: 'Professional interview',
      duration: '4 weeks',
      progress: 35,
      lessons: interviewLessons,
    },
    {
      title: 'Italy Travel Survival Plan',
      description: 'A two-week daily plan for practical Italian before a trip.',
      summary:
        'Prepare practical travel language for restaurants, directions, hotels, and simple travel problems with daily short lessons.',
      language: 'Italian',
      goal: 'Travel in two weeks',
      duration: '14 days',
      progress: 18,
      lessons: italyLessons,
    },
  ];
  const activePlan = learningPlans[selectedPlan];
  const activeLesson = activePlan.lessons[selectedLesson] ?? activePlan.lessons[0];
  const ActiveLessonIcon = activeLesson.icon;
  const recentLessons = learningPlans
    .flatMap((plan, planIndex) =>
      plan.lessons.map((lesson, lessonIndex) => ({
        ...lesson,
        planIndex,
        lessonIndex,
        planTitle: plan.title,
      })),
    )
    .sort((a, b) => b.progress - a.progress)
    .slice(0, 3);
  const weeklyPlan = [
    { day: 'M', done: true },
    { day: 'T', done: true },
    { day: 'W', done: false },
    { day: 'T', done: true },
    { day: 'F', done: false },
    { day: 'S', done: false },
    { day: 'S', done: false },
  ];

  return (
    <main className="phone-stage">
      <div className="phone-frame" aria-label="Phone app preview">
        <div className="phone-speaker" aria-hidden="true"></div>

        <section className={`app-shell ${darkMode ? 'dark-mode' : ''}`} aria-label="APP_NAME">
          <div className="status-bar" aria-hidden="true">
            <span>9:41</span>
            <span>LTE 100%</span>
          </div>

          {screen === 'auth' ? (
            <>
              <header className="app-header">
                <div className="logo-placeholder" aria-label="Logo placeholder">
                  TD
                </div>
                <div>
                  <p className="app-label">Language learning</p>
                  <h1>APP_NAME</h1>
                </div>
              </header>

              <section className="auth-card" aria-label="Sign in options">
                <div className="auth-heading">
                  <p className="eyebrow">Welcome back</p>
                  <h2>Sign in to continue learning.</h2>
                </div>

                <div className="auth-actions">
                  <button className="auth-button gmail-button" type="button" onClick={goToMain}>
                    <span className="google-mark" aria-hidden="true">
                      G
                    </span>
                    Continue with Gmail
                  </button>

                  <button className="auth-button register-button" type="button" onClick={goToMain}>
                    <UserPlus size={20} aria-hidden="true" />
                    Register
                  </button>
                </div>

                <div className="divider">
                  <span>or</span>
                </div>

                <button className="text-login" type="button" onClick={goToMain}>
                  <Mail size={18} aria-hidden="true" />
                  Sign in with email
                </button>
              </section>
            </>
          ) : screen === 'main' ? (
            <section className="main-page" aria-label="Overview dashboard">
              <header className="main-topbar">
                <div>
                  <p className="app-label">Welcome back</p>
                  <h2>Dashboard</h2>
                </div>
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Settings"
                  onClick={openSettings}
                >
                  <Settings size={22} aria-hidden="true" />
                </button>
              </header>

              <div className="dashboard-content">
                <section className="dashboard-hero" aria-label="Weekly overview">
                  <div>
                    <p>{activePlan.language} plan</p>
                    <h3>{activePlan.title}</h3>
                    <span>{activePlan.lessons.length} lessons in progress</span>
                  </div>
                  <div
                    className="hero-progress"
                    aria-label={`Overall progress ${activePlan.progress} percent`}
                  >
                    <strong>{activePlan.progress}%</strong>
                    <small>Overall</small>
                  </div>
                </section>

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

                <section className="lesson-progress-card" aria-label="Lesson progress">
                  <div className="section-heading">
                    <h3>Recent progress</h3>
                    <span>Top 3 lessons</span>
                  </div>
                  {recentLessons.map((lesson, index) => (
                    <button
                      className="progress-row progress-link"
                      key={`${lesson.planTitle}-${lesson.title}`}
                      type="button"
                      onClick={() => {
                        setSelectedPlan(lesson.planIndex);
                        setSelectedLesson(lesson.lessonIndex);
                        setLearningMode('training');
                        setLearningStep('lesson');
                        setScreen('learn');
                      }}
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
                  ))}
                </section>
              </div>

              <nav className="bottom-bar" aria-label="Main navigation">
                <button className="tab-button" type="button" onClick={() => setScreen('learn')}>
                  <BookOpen size={21} aria-hidden="true" />
                  <span>Start learning</span>
                </button>
                <button className="tab-button active" type="button" onClick={() => setScreen('main')}>
                  <CalendarDays size={21} aria-hidden="true" />
                  <span>Overview</span>
                </button>
                <button className="tab-button" type="button">
                  <Clock3 size={21} aria-hidden="true" />
                  <span>History</span>
                </button>
              </nav>
            </section>
          ) : screen === 'learn' ? (
            <section className="main-page" aria-label="Start learning">
              <header className="main-topbar">
                <div className="topbar-title">
                  {learningMode === 'training' && learningStep !== 'plans' && (
                    <button
                      className="inline-back"
                      type="button"
                      aria-label="Back"
                      onClick={() =>
                        setLearningStep(
                          learningStep === 'lesson' || learningStep === 'planDetail'
                            ? 'lessons'
                            : 'plans',
                        )
                      }
                    >
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
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Settings"
                  onClick={openSettings}
                >
                  <Settings size={22} aria-hidden="true" />
                </button>
              </header>

              <div className="learning-content">
                <div className="learning-options" aria-label="Learning type">
                  <button
                    className={`mode-card ${learningMode === 'training' ? 'selected' : ''}`}
                    type="button"
                    onClick={() => {
                      setLearningMode('training');
                      setLearningStep('plans');
                    }}
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
                    onClick={() => {
                      setLearningMode('ai');
                      setLearningStep('plans');
                    }}
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
                    {learningStep === 'plans' && (
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
                              <button
                                className="plan-card"
                                key={plan.title}
                                type="button"
                                onClick={() => {
                                  setSelectedPlan(planIndex);
                                  setSelectedLesson(0);
                                  setLearningStep('lessons');
                                }}
                              >
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
                    )}

                    {learningStep === 'lessons' && (
                      <>
                        <button
                          className="lesson-overview plan-overview-button"
                          type="button"
                          aria-label="Open learning plan overview"
                          onClick={() => setLearningStep('planDetail')}
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
                              <button
                                className="exercise-card"
                                key={lesson.title}
                                type="button"
                                onClick={() => {
                                  setSelectedLesson(index);
                                  setLearningStep('lesson');
                                }}
                              >
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
                    )}

                    {learningStep === 'planDetail' && (
                      <section className="plan-detail" aria-label="Learning plan overview">
                        <section className="dashboard-hero compact-hero">
                          <div>
                            <p>{activePlan.duration}</p>
                            <h3>{activePlan.title}</h3>
                            <span>{activePlan.goal}</span>
                          </div>
                          <div
                            className="hero-progress"
                            aria-label={`Overall progress ${activePlan.progress} percent`}
                          >
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
                    )}

                    {learningStep === 'lesson' && (
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
                    )}
                  </>
                ) : (
                  <section className="ai-placeholder" aria-label="AI Training placeholder">
                    <BrainCircuit size={34} aria-hidden="true" />
                    <h3>AI Training</h3>
                    <p>Personalized AI practice will live here later. For now, continue with Training lessons.</p>
                  </section>
                )}
              </div>

              <nav className="bottom-bar" aria-label="Main navigation">
                <button className="tab-button active" type="button" onClick={() => setScreen('learn')}>
                  <BookOpen size={21} aria-hidden="true" />
                  <span>Start learning</span>
                </button>
                <button className="tab-button" type="button" onClick={() => setScreen('main')}>
                  <CalendarDays size={21} aria-hidden="true" />
                  <span>Overview</span>
                </button>
                <button className="tab-button" type="button">
                  <Clock3 size={21} aria-hidden="true" />
                  <span>History</span>
                </button>
              </nav>
            </section>
          ) : screen === 'profile' ? (
            <section className="profile-page" aria-label="Personal profile">
              <header className="page-topbar">
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Back to main page"
                  onClick={goToMain}
                >
                  <ChevronLeft size={22} aria-hidden="true" />
                </button>
                <h2>Profile</h2>
                <span aria-hidden="true"></span>
              </header>

              <div className="profile-content">
                <div className="profile-avatar" aria-label="Profile picture placeholder">
                  <User size={38} aria-hidden="true" />
                </div>
                <button className="change-photo-button" type="button">
                  Change photo
                </button>

                <div className="profile-fields">
                  <div className="profile-field">
                    <span>Name</span>
                    <strong>APP_USER</strong>
                  </div>
                  <div className="profile-field">
                    <span>Country</span>
                    <strong>Germany</strong>
                  </div>
                </div>

                <button className="edit-profile-button" type="button">
                  Edit
                </button>
              </div>
            </section>
          ) : (
            <section className="language-page" aria-label="Language selection">
              <header className="page-topbar">
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Back to main page"
                  onClick={goToMain}
                >
                  <ChevronLeft size={22} aria-hidden="true" />
                </button>
                <h2>Language</h2>
                <span aria-hidden="true"></span>
              </header>

              <div className="language-content">
                <p className="language-heading">Choose app language</p>
                <div className="language-bubbles">
                  {languages.map((item) => (
                    <button
                      className={`language-bubble ${language === item ? 'selected' : ''}`}
                      key={item}
                      type="button"
                      onClick={() => setLanguage(item)}
                    >
                      {item}
                    </button>
                  ))}
                </div>
              </div>
            </section>
          )}

          {settingsOpen && (
            <div
              className={`settings-overlay ${settingsClosing ? 'closing' : ''}`}
              role="presentation"
            >
              <button
                className="settings-backdrop"
                type="button"
                aria-label="Close settings"
                onClick={() => closeSettings()}
              ></button>

              <aside className="settings-sidebar" aria-label="Settings sidebar">
                <header className="sidebar-topbar">
                  <h2>Settings</h2>
                  <button
                    className="icon-button"
                    type="button"
                    aria-label="Close settings"
                    onClick={() => closeSettings()}
                  >
                    <X size={21} aria-hidden="true" />
                  </button>
                </header>

                <div className="settings-list">
                  <button className="settings-row" type="button" onClick={openProfile}>
                    <span className="row-icon">
                      <User size={20} aria-hidden="true" />
                    </span>
                    <span className="row-text">
                      <strong>Personal profile</strong>
                      <small>View and edit account details</small>
                    </span>
                  </button>

                  <button className="settings-row" type="button" onClick={openLanguage}>
                    <span className="row-icon">
                      <Languages size={20} aria-hidden="true" />
                    </span>
                    <span className="row-text">
                      <strong>Language</strong>
                      <small>{language}</small>
                    </span>
                  </button>

                  <div className="settings-row">
                    <span className="row-icon">
                      {darkMode ? (
                        <Moon size={20} aria-hidden="true" />
                      ) : (
                        <Sun size={20} aria-hidden="true" />
                      )}
                    </span>
                    <span className="row-text">
                      <strong>Light / dark mode</strong>
                      <small>{darkMode ? 'Dark mode enabled' : 'Light mode enabled'}</small>
                    </span>
                    <button
                      className={`switch ${darkMode ? 'on' : ''}`}
                      type="button"
                      aria-label="Toggle light and dark mode"
                      onClick={() => setDarkMode((value) => !value)}
                    >
                      <span></span>
                    </button>
                  </div>
                </div>

                <button className="logout-button" type="button" onClick={logOut}>
                  <LogOut size={20} aria-hidden="true" />
                  Log out
                </button>
              </aside>
            </div>
          )}

          <div className="home-indicator" aria-hidden="true"></div>
        </section>
      </div>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
