import React from 'react';
import { SettingsOverlay } from './components/SettingsOverlay';
import { languages } from './data/languages';
import { learningPlans, weeklyPlan } from './data/learningPlans';
import { AuthPage } from './pages/AuthPage';
import { DashboardPage } from './pages/DashboardPage';
import { LanguagePage } from './pages/LanguagePage';
import { LearningPage } from './pages/LearningPage';
import { ProfilePage } from './pages/ProfilePage';
import { getRecentLessons } from './utils/learning';

export function App() {
  const [screen, setScreen] = React.useState('auth');
  const [settingsOpen, setSettingsOpen] = React.useState(false);
  const [settingsClosing, setSettingsClosing] = React.useState(false);
  const [darkMode, setDarkMode] = React.useState(false);
  const [language, setLanguage] = React.useState('English');
  const [selectedPlan, setSelectedPlan] = React.useState(0);
  const [selectedLesson, setSelectedLesson] = React.useState(0);
  const [selectedExercise, setSelectedExercise] = React.useState(0);
  const [learningMode, setLearningMode] = React.useState('training');
  const [learningStep, setLearningStep] = React.useState('plans');

  const activePlan = learningPlans[selectedPlan];
  const activeLesson = activePlan.lessons[selectedLesson] ?? activePlan.lessons[0];
  const activeExercise = activeLesson.exercises[selectedExercise] ?? activeLesson.exercises[0];
  const recentLessons = getRecentLessons(learningPlans);

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

  const openLessonFromDashboard = (planIndex, lessonIndex) => {
    setSelectedPlan(planIndex);
    setSelectedLesson(lessonIndex);
    setSelectedExercise(0);
    setLearningMode('training');
    setLearningStep('lesson');
    setScreen('learn');
  };

  const openPlanFromDashboard = () => {
    setSelectedLesson(0);
    setLearningMode('training');
    setLearningStep('lessons');
    setScreen('learn');
  };

  const openPlan = (planIndex) => {
    setSelectedPlan(planIndex);
    setSelectedLesson(0);
    setSelectedExercise(0);
    setLearningStep('lessons');
  };

  const openLesson = (lessonIndex) => {
    setSelectedLesson(lessonIndex);
    setSelectedExercise(0);
    setLearningStep('lesson');
  };

  const openExercise = (exerciseIndex) => {
    setSelectedExercise(exerciseIndex);
    setLearningStep('exercise');
  };

  const changeLearningMode = (nextMode) => {
    setLearningMode(nextMode);
    setLearningStep('plans');
  };

  const goBackInLearning = () => {
    if (learningStep === 'exercise') {
      setLearningStep('lesson');
      return;
    }

    setLearningStep(learningStep === 'lesson' || learningStep === 'planDetail' ? 'lessons' : 'plans');
  };

  return (
    <main className="phone-stage">
      <div className="phone-frame" aria-label="Phone app preview">
        <div className="phone-speaker" aria-hidden="true"></div>

        <section className={`app-shell ${darkMode ? 'dark-mode' : ''}`} aria-label="APP_NAME">
          <div className="status-bar" aria-hidden="true">
            <span>9:41</span>
            <span>LTE 100%</span>
          </div>

          {screen === 'auth' && <AuthPage onAuthenticated={goToMain} />}

          {screen === 'main' && (
            <DashboardPage
              activePlan={activePlan}
              recentLessons={recentLessons}
              weeklyPlan={weeklyPlan}
              onNavigate={setScreen}
              onOpenLesson={openLessonFromDashboard}
              onOpenPlan={openPlanFromDashboard}
              onOpenSettings={openSettings}
            />
          )}

          {screen === 'learn' && (
            <LearningPage
              activeExercise={activeExercise}
              activeLesson={activeLesson}
              activePlan={activePlan}
              learningMode={learningMode}
              learningPlans={learningPlans}
              learningStep={learningStep}
              onBack={goBackInLearning}
              onLearningMode={changeLearningMode}
              onNavigate={setScreen}
              onOpenExercise={openExercise}
              onOpenLesson={openLesson}
              onOpenPlan={openPlan}
              onOpenPlanDetail={() => setLearningStep('planDetail')}
              onOpenSettings={openSettings}
            />
          )}

          {screen === 'profile' && <ProfilePage onBack={goToMain} />}

          {screen === 'language' && (
            <LanguagePage
              language={language}
              languages={languages}
              onBack={goToMain}
              onLanguage={setLanguage}
            />
          )}

          {settingsOpen && (
            <SettingsOverlay
              closing={settingsClosing}
              darkMode={darkMode}
              language={language}
              onClose={() => closeSettings()}
              onLanguage={() => closeSettings('language')}
              onLogout={() => closeSettings('auth')}
              onProfile={() => closeSettings('profile')}
              onToggleDarkMode={() => setDarkMode((value) => !value)}
            />
          )}

          <div className="home-indicator" aria-hidden="true"></div>
        </section>
      </div>
    </main>
  );
}
