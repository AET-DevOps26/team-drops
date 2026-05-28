import React from 'react';
import { SettingsOverlay } from './components/SettingsOverlay';
import { languages, targetLanguages } from './data/languages';
import { learningPlans, weeklyPlan } from './data/learningPlans';
import { AuthPage } from './pages/AuthPage';
import { DashboardPage } from './pages/DashboardPage';
import { LanguagePage } from './pages/LanguagePage';
import { LearningPage } from './pages/LearningPage';
import { ProfilePage } from './pages/ProfilePage';
import { getRecentFinishedLessons, getRecentLessons } from './utils/learning';

const translations = {
  English: {
    appLabel: 'Language learning',
    welcomeBack: 'Welcome back',
    dashboard: 'Dashboard',
    settings: 'Settings',
    startLearning: 'Start learning',
    overview: 'Overview',
    language: 'Language',
    languageToLearn: 'Language to learn',
    appLanguage: 'Choose app language',
    targetLanguage: 'Choose language to learn',
    profile: 'Profile',
    name: 'Name',
    country: 'Country',
    edit: 'Edit',
    save: 'Save',
    learn: 'Learning Hub',
    learningPlan: 'Learning Plan',
    lesson: 'Lesson',
    exercise: 'Exercise',
    planOverview: 'Plan overview',
    currentPlan: 'plan',
    lessonsInProgress: 'lessons in progress',
    exercisesDone: 'Exercises done',
    averageScore: 'Average score',
    recentProgress: 'Recent progress',
    recentFinish: 'Recent finish',
    topLessons: 'Top 3 lessons',
    noOngoing: 'No ongoing lessons yet',
    noFinished: 'No finished lessons yet',
    personalProfile: 'Personal profile',
    accountDetails: 'View and edit account details',
    appearance: 'Light / dark mode',
    darkEnabled: 'Dark mode enabled',
    lightEnabled: 'Light mode enabled',
    logout: 'Log out',
    training: 'Training',
    trainingDescription: 'Guided interview lessons',
    aiTraining: 'AI Training',
    comingLater: 'Coming later',
    suggestedPlans: 'Suggested learning plans',
    plans: 'plans',
    overall: 'Overall',
    comingSoon: 'Coming soon',
  },
  German: {
    appLabel: 'Sprachen lernen',
    welcomeBack: 'Willkommen zuruck',
    dashboard: 'Ubersicht',
    settings: 'Einstellungen',
    startLearning: 'Lernen starten',
    overview: 'Ubersicht',
    language: 'Sprache',
    languageToLearn: 'Lernsprache',
    appLanguage: 'App-Sprache wahlen',
    targetLanguage: 'Lernsprache wahlen',
    profile: 'Profil',
    name: 'Name',
    country: 'Land',
    edit: 'Bearbeiten',
    save: 'Speichern',
    learn: 'Lernbereich',
    learningPlan: 'Lernplan',
    lesson: 'Lektion',
    exercise: 'Ubung',
    planOverview: 'Planubersicht',
    currentPlan: 'Plan',
    lessonsInProgress: 'Lektionen laufen',
    exercisesDone: 'Ubungen erledigt',
    averageScore: 'Durchschnitt',
    recentProgress: 'Aktueller Fortschritt',
    recentFinish: 'Abgeschlossen',
    topLessons: 'Top 3 Lektionen',
    noOngoing: 'Noch keine laufenden Lektionen',
    noFinished: 'Noch keine abgeschlossenen Lektionen',
    personalProfile: 'Personliches Profil',
    accountDetails: 'Kontodaten anzeigen und bearbeiten',
    appearance: 'Hell- / Dunkelmodus',
    darkEnabled: 'Dunkelmodus aktiv',
    lightEnabled: 'Hellmodus aktiv',
    logout: 'Abmelden',
    training: 'Training',
    trainingDescription: 'Gefuhrte Interviewlektionen',
    aiTraining: 'KI-Training',
    comingLater: 'Kommt spater',
    suggestedPlans: 'Vorgeschlagene Lernplane',
    plans: 'Plane',
    overall: 'Gesamt',
    comingSoon: 'Kommt bald',
  },
  French: {
    appLabel: 'Apprentissage des langues',
    welcomeBack: 'Bon retour',
    dashboard: 'Tableau de bord',
    settings: 'Parametres',
    startLearning: 'Commencer',
    overview: 'Vue d ensemble',
    language: 'Langue',
    languageToLearn: 'Langue a apprendre',
    appLanguage: 'Choisir la langue de l app',
    targetLanguage: 'Choisir la langue a apprendre',
    profile: 'Profil',
    name: 'Nom',
    country: 'Pays',
    edit: 'Modifier',
    save: 'Enregistrer',
    learn: 'Espace apprentissage',
    learningPlan: 'Plan d apprentissage',
    lesson: 'Lecon',
    exercise: 'Exercice',
    planOverview: 'Apercu du plan',
    currentPlan: 'plan',
    lessonsInProgress: 'lecons en cours',
    exercisesDone: 'Exercices faits',
    averageScore: 'Score moyen',
    recentProgress: 'Progression recente',
    recentFinish: 'Termine recemment',
    topLessons: 'Top 3 lecons',
    noOngoing: 'Aucune lecon en cours',
    noFinished: 'Aucune lecon terminee',
    personalProfile: 'Profil personnel',
    accountDetails: 'Voir et modifier le compte',
    appearance: 'Mode clair / sombre',
    darkEnabled: 'Mode sombre active',
    lightEnabled: 'Mode clair active',
    logout: 'Se deconnecter',
    training: 'Entrainement',
    trainingDescription: 'Lecons guidees d entretien',
    aiTraining: 'Entrainement IA',
    comingLater: 'A venir',
    suggestedPlans: 'Plans suggeres',
    plans: 'plans',
    overall: 'Total',
    comingSoon: 'Bientot disponible',
  },
};

export function App() {
  const [screen, setScreen] = React.useState('auth');
  const [settingsOpen, setSettingsOpen] = React.useState(false);
  const [settingsClosing, setSettingsClosing] = React.useState(false);
  const [darkMode, setDarkMode] = React.useState(false);
  const [language, setLanguage] = React.useState('English');
  const [targetLanguage, setTargetLanguage] = React.useState('German');
  const [profile, setProfile] = React.useState({ name: 'Dr. Ops', country: 'Germany' });
  const [selectedPlan, setSelectedPlan] = React.useState(0);
  const [selectedLesson, setSelectedLesson] = React.useState(0);
  const [selectedExercise, setSelectedExercise] = React.useState(0);
  const [learningMode, setLearningMode] = React.useState('training');
  const [learningStep, setLearningStep] = React.useState('plans');

  const activePlan = learningPlans[selectedPlan];
  const activeLesson = activePlan.lessons[selectedLesson] ?? activePlan.lessons[0];
  const activeExercise = activeLesson.exercises[selectedExercise] ?? activeLesson.exercises[0];
  const recentLessons = getRecentLessons(learningPlans);
  const recentFinishedLessons = getRecentFinishedLessons(learningPlans);
  const t = translations[language] ?? translations.English;

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

          {screen === 'auth' && <AuthPage t={t} onAuthenticated={goToMain} />}

          {screen === 'main' && (
            <DashboardPage
              activePlan={activePlan}
              recentFinishedLessons={recentFinishedLessons}
              recentLessons={recentLessons}
              profile={profile}
              weeklyPlan={weeklyPlan}
              t={t}
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
              t={t}
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

          {screen === 'profile' && (
            <ProfilePage profile={profile} t={t} onBack={goToMain} onProfileChange={setProfile} />
          )}

          {screen === 'language' && (
            <LanguagePage
              heading={t.appLanguage}
              language={language}
              languages={languages}
              title={t.language}
              onBack={goToMain}
              onLanguage={setLanguage}
            />
          )}

          {screen === 'targetLanguage' && (
            <LanguagePage
              heading={t.targetLanguage}
              language={targetLanguage}
              languages={targetLanguages}
              title={t.languageToLearn}
              onBack={goToMain}
              onLanguage={setTargetLanguage}
            />
          )}

          {settingsOpen && (
            <SettingsOverlay
              closing={settingsClosing}
              darkMode={darkMode}
              language={language}
              targetLanguage={targetLanguage}
              t={t}
              onClose={() => closeSettings()}
              onLanguage={() => closeSettings('language')}
              onTargetLanguage={() => closeSettings('targetLanguage')}
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
