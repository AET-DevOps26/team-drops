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
  MessageSquareText,
  Moon,
  PenLine,
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
  const [selectedExercise, setSelectedExercise] = React.useState(0);
  const [learningMode, setLearningMode] = React.useState('training');

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
  const interviewExercises = [
    {
      title: 'Self Introduction',
      topic: 'Introduce yourself professionally in an interview.',
      icon: MessageSquareText,
      accent: 'blue',
      time: '8 min',
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
      exercises: [
        'Name two strengths with examples.',
        'Explain one weakness professionally.',
        'Rewrite weak answers into stronger interview answers.',
      ],
    },
  ];
  const activeExercise = interviewExercises[selectedExercise];
  const ActiveExerciseIcon = activeExercise.icon;
  const weeklyPlan = [
    { day: 'M', done: true },
    { day: 'T', done: true },
    { day: 'W', done: false },
    { day: 'T', done: true },
    { day: 'F', done: false },
    { day: 'S', done: false },
    { day: 'S', done: false },
  ];
  const lessonProgress = [60, 20, 0, 0, 0];

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
                    <p>German Job Interview</p>
                    <h3>Preparation Plan</h3>
                    <span>3 lessons scheduled this week</span>
                  </div>
                  <div className="hero-progress" aria-label="Overall progress 35 percent">
                    <strong>35%</strong>
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
                    <h3>Lesson progress</h3>
                    <span>5 lessons</span>
                  </div>
                  {interviewExercises.slice(0, 3).map((exercise, index) => (
                    <div className="progress-row" key={exercise.title}>
                      <span className={`mini-dot ${exercise.accent}`}>{index + 1}</span>
                      <div>
                        <strong>{exercise.title}</strong>
                        <div className="progress-track" aria-hidden="true">
                          <span style={{ width: `${lessonProgress[index]}%` }}></span>
                        </div>
                      </div>
                      <small>{lessonProgress[index]}%</small>
                    </div>
                  ))}
                </section>
              </div>

              <nav className="bottom-bar" aria-label="Main navigation">
                <button className="tab-button active" type="button" onClick={() => setScreen('main')}>
                  <CalendarDays size={21} aria-hidden="true" />
                  <span>Overview</span>
                </button>
                <button className="tab-button" type="button" onClick={() => setScreen('learn')}>
                  <BookOpen size={21} aria-hidden="true" />
                  <span>Start learning</span>
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
                <div>
                  <p className="app-label">Start learning</p>
                  <h2>Learning Hub</h2>
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
                    onClick={() => setLearningMode('training')}
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
                    onClick={() => setLearningMode('ai')}
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
                    <section className="lesson-overview" aria-label="Selected lesson">
                      <div className={`lesson-icon ${activeExercise.accent}`}>
                        <ActiveExerciseIcon size={24} aria-hidden="true" />
                      </div>
                      <div className="lesson-summary">
                        <p>{activeExercise.time}</p>
                        <h3>{activeExercise.title}</h3>
                        <span>{activeExercise.topic}</span>
                      </div>
                    </section>

                    <div className="exercise-list" aria-label="Training lessons">
                      {interviewExercises.map((exercise, index) => {
                        const ExerciseIcon = exercise.icon;

                        return (
                          <button
                            className={`exercise-card ${
                              selectedExercise === index ? 'selected' : ''
                            }`}
                            key={exercise.title}
                            type="button"
                            onClick={() => setSelectedExercise(index)}
                          >
                            <span className={`module-icon ${exercise.accent}`}>
                              <ExerciseIcon size={19} aria-hidden="true" />
                            </span>
                            <span className="module-copy">
                              <strong>{exercise.title}</strong>
                              <small>{exercise.topic}</small>
                            </span>
                            <span className="lesson-percent">{lessonProgress[index]}%</span>
                          </button>
                        );
                      })}
                    </div>

                    <section className="prompt-panel" aria-label="Practice prompts">
                      <div className="prompt-heading">
                        <PenLine size={18} aria-hidden="true" />
                        <h3>Practice tasks</h3>
                      </div>

                      <ol className="prompt-list">
                        {activeExercise.exercises.map((exercise) => (
                          <li key={exercise}>{exercise}</li>
                        ))}
                      </ol>
                    </section>
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
                <button className="tab-button" type="button" onClick={() => setScreen('main')}>
                  <CalendarDays size={21} aria-hidden="true" />
                  <span>Overview</span>
                </button>
                <button className="tab-button active" type="button" onClick={() => setScreen('learn')}>
                  <BookOpen size={21} aria-hidden="true" />
                  <span>Start learning</span>
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
