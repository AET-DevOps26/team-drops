import React from 'react';
import {
  createAiLearningPlan,
  createDefaultLearningPlan,
  generateListeningContent,
  getCurrentUser,
  getLearningPlans,
  getLesson,
  getFeedbackByAnswerId,
  getProgress,
  getUserProfile,
  getUserAnswers,
  submitAnswer,
  submitSpeakingAnswer,
  updateUserProfile,
} from './api/client';
import {
  authEnabled,
  getValidAccessToken,
  initializeAuth,
  loginWithKeycloak,
  logoutFromKeycloak,
  registerWithKeycloak,
} from './auth/keycloak';
import {
  attachSubmissionToLesson,
  attachSavedAnswersToLesson,
  createEmptyProgress,
  derivePlanProgress,
  mergeLessonIntoPlans,
  toLearningPlans,
  toLessonDetail,
  toProfile,
  toProgressSummary,
} from './api/mappers';
import { IntroOverlay } from './components/IntroOverlay';
import { LanguageSwitchOverlay } from './components/LanguageSwitchOverlay';
import { SettingsOverlay } from './components/SettingsOverlay';
import { languages, targetLanguages } from './data/languages';
import { AuthPage } from './pages/AuthPage';
import { DashboardPage } from './pages/DashboardPage';
import { LanguagePage } from './pages/LanguagePage';
import { LearningPage } from './pages/LearningPage';
import { ProfilePage } from './pages/ProfilePage';
import { hasReviewedAnswer } from './utils/exercises';
import { getRecentFinishedLessons, getRecentLessons } from './utils/learning';

const defaultProfile = {
  name: 'Learner',
  country: 'Unknown',
  targetLanguage: 'German',
  currentLevel: 'A2',
  learningGoal: 'Prepare for a software engineering job interview',
};

function isReviewedListeningExercise(exercise) {
  return exercise?.type === 'listening' && hasReviewedAnswer(exercise);
}

const introPendingKey = 'teamDropsIntroPending';

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
    recentProgress: 'Recent progress',
    recentFinish: 'Recent finish',
    topLessons: 'Top 3 lessons',
    noOngoing: 'No ongoing lessons yet',
    noFinished: 'No finished lessons yet',
    statusNotStarted: 'Not started',
    statusOngoing: 'Ongoing',
    statusFinished: 'Finished',
    personalProfile: 'Personal profile',
    accountDetails: 'View and edit account details',
    appearance: 'Light / dark mode',
    darkEnabled: 'Dark mode enabled',
    lightEnabled: 'Light mode enabled',
    logout: 'Log out',
    training: 'Training',
    trainingDescription: 'Guided interview lessons',
    aiTraining: 'AI Training',
    aiTrainingDescription: 'Adaptive interview practice will be available here.',
    ragLearning: 'RAG learning',
    ragLearningDescription: 'Create from document topics',
    chooseRagTopic: 'Choose a RAG topic',
    generateLearningPlan: 'Generate learning plan',
    comingLater: 'Coming later',
    suggestedPlans: 'Suggested learning plans',
    plans: 'plans',
    overall: 'Overall',
    comingSoon: 'Coming soon',
    switchingLanguage: 'Switching learning language',
    loadingLanguageContent: 'Loading your {language} plans, lessons, and progress.',
  },
  German: {
    appLabel: 'Sprachen lernen',
    welcomeBack: 'Willkommen zurück',
    dashboard: 'Übersicht',
    settings: 'Einstellungen',
    startLearning: 'Lernen starten',
    overview: 'Übersicht',
    language: 'Sprache',
    languageToLearn: 'Lernsprache',
    appLanguage: 'App-Sprache wählen',
    targetLanguage: 'Lernsprache wählen',
    profile: 'Profil',
    name: 'Name',
    country: 'Land',
    edit: 'Bearbeiten',
    save: 'Speichern',
    learn: 'Lernbereich',
    learningPlan: 'Lernplan',
    lesson: 'Lektion',
    exercise: 'Übung',
    planOverview: 'Planübersicht',
    currentPlan: 'Plan',
    lessonsInProgress: 'Lektionen laufen',
    recentProgress: 'Aktueller Fortschritt',
    recentFinish: 'Abgeschlossen',
    topLessons: 'Top 3 Lektionen',
    noOngoing: 'Noch keine laufenden Lektionen',
    noFinished: 'Noch keine abgeschlossenen Lektionen',
    statusNotStarted: 'Noch nicht begonnen',
    statusOngoing: 'In Bearbeitung',
    statusFinished: 'Abgeschlossen',
    personalProfile: 'Persönliches Profil',
    accountDetails: 'Kontodaten anzeigen und bearbeiten',
    appearance: 'Hell- / Dunkelmodus',
    darkEnabled: 'Dunkelmodus aktiv',
    lightEnabled: 'Hellmodus aktiv',
    logout: 'Abmelden',
    training: 'Training',
    trainingDescription: 'Geführte Interviewlektionen',
    aiTraining: 'KI-Training',
    aiTrainingDescription: 'Adaptives Interviewtraining wird hier verfügbar sein.',
    ragLearning: 'RAG-Lernen',
    ragLearningDescription: 'Aus Dokumentthemen erstellen',
    chooseRagTopic: 'RAG-Thema wählen',
    generateLearningPlan: 'Lernplan erstellen',
    comingLater: 'Kommt später',
    suggestedPlans: 'Vorgeschlagene Lernpläne',
    plans: 'Pläne',
    overall: 'Gesamt',
    comingSoon: 'Kommt bald',
    switchingLanguage: 'Lernsprache wird gewechselt',
    loadingLanguageContent: 'Deine Inhalte, Lektionen und Fortschritte für {language} werden geladen.',
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
    recentProgress: 'Progression recente',
    recentFinish: 'Termine recemment',
    topLessons: 'Top 3 lecons',
    noOngoing: 'Aucune lecon en cours',
    noFinished: 'Aucune lecon terminee',
    statusNotStarted: 'Pas commence',
    statusOngoing: 'En cours',
    statusFinished: 'Termine',
    personalProfile: 'Profil personnel',
    accountDetails: 'Voir et modifier le compte',
    appearance: 'Mode clair / sombre',
    darkEnabled: 'Mode sombre active',
    lightEnabled: 'Mode clair active',
    logout: 'Se deconnecter',
    training: 'Entrainement',
    trainingDescription: 'Lecons guidees d entretien',
    aiTraining: 'Entrainement IA',
    aiTrainingDescription: 'La pratique adaptive d entretien sera disponible ici.',
    ragLearning: 'Apprentissage RAG',
    ragLearningDescription: 'Creer depuis les documents',
    chooseRagTopic: 'Choisir un sujet RAG',
    generateLearningPlan: 'Generer un plan',
    comingLater: 'A venir',
    suggestedPlans: 'Plans suggeres',
    plans: 'plans',
    overall: 'Total',
    comingSoon: 'Bientot disponible',
    switchingLanguage: 'Changement de langue',
    loadingLanguageContent: 'Chargement des plans, lecons et progres en {language}.',
  },
};

export function App() {
  const [screen, setScreen] = React.useState('auth');
  const [settingsOpen, setSettingsOpen] = React.useState(false);
  const [settingsClosing, setSettingsClosing] = React.useState(false);
  const [introOpen, setIntroOpen] = React.useState(false);
  const [darkMode, setDarkMode] = React.useState(false);
  const [language, setLanguage] = React.useState('English');
  const [targetLanguage, setTargetLanguage] = React.useState(defaultProfile.targetLanguage);
  const [session, setSession] = React.useState(null);
  const [authPending, setAuthPending] = React.useState(false);
  const [authError, setAuthError] = React.useState('');
  const [loadingInitialData, setLoadingInitialData] = React.useState(false);
  const [switchingTargetLanguage, setSwitchingTargetLanguage] = React.useState('');
  const [dashboardError, setDashboardError] = React.useState('');
  const [profilePending, setProfilePending] = React.useState(false);
  const [profileError, setProfileError] = React.useState('');
  const [answerPending, setAnswerPending] = React.useState(false);
  const [answerError, setAnswerError] = React.useState('');
  const [learningError, setLearningError] = React.useState('');
  const [profile, setProfile] = React.useState(defaultProfile);
  const [progress, setProgress] = React.useState(createEmptyProgress());
  const [learningPlans, setLearningPlans] = React.useState([]);
  const [selectedPlan, setSelectedPlan] = React.useState(0);
  const [selectedLesson, setSelectedLesson] = React.useState(0);
  const [selectedExercise, setSelectedExercise] = React.useState(0);
  const [learningMode, setLearningMode] = React.useState('training');
  const [learningStep, setLearningStep] = React.useState('plans');
  const [listeningContent, setListeningContent] = React.useState(null);
  const [listeningSelections, setListeningSelections] = React.useState({});
  const [listeningLoading, setListeningLoading] = React.useState(false);
  const [listeningError, setListeningError] = React.useState('');
  const listeningRequestIdRef = React.useRef(0);
  const screenRef = React.useRef(screen);
  const selectedPlanRef = React.useRef(selectedPlan);
  const selectedLessonRef = React.useRef(selectedLesson);
  const selectedExerciseRef = React.useRef(selectedExercise);
  const targetLanguageRef = React.useRef(targetLanguage);
  const initialAuthRedirectDoneRef = React.useRef(false);

  React.useEffect(() => {
    screenRef.current = screen;
  }, [screen]);

  React.useEffect(() => {
    selectedPlanRef.current = selectedPlan;
  }, [selectedPlan]);

  React.useEffect(() => {
    selectedLessonRef.current = selectedLesson;
  }, [selectedLesson]);

  React.useEffect(() => {
    selectedExerciseRef.current = selectedExercise;
  }, [selectedExercise]);

  React.useEffect(() => {
    targetLanguageRef.current = targetLanguage;
  }, [targetLanguage]);

  const navigateToScreen = React.useCallback((nextScreen) => {
    screenRef.current = nextScreen;
    setScreen(nextScreen);
  }, []);

  const selectPlan = React.useCallback((planIndex) => {
    selectedPlanRef.current = planIndex;
    setSelectedPlan(planIndex);
  }, []);

  const selectLesson = React.useCallback((lessonIndex) => {
    selectedLessonRef.current = lessonIndex;
    setSelectedLesson(lessonIndex);
  }, []);

  const selectExercise = React.useCallback((exerciseIndex) => {
    selectedExerciseRef.current = exerciseIndex;
    setSelectedExercise(exerciseIndex);
  }, []);

  const setTargetLanguageValue = React.useCallback((nextTargetLanguage) => {
    targetLanguageRef.current = nextTargetLanguage;
    setTargetLanguage(nextTargetLanguage);
  }, []);

  const resetLearningSelection = React.useCallback(() => {
    selectedPlanRef.current = 0;
    selectedLessonRef.current = 0;
    selectedExerciseRef.current = 0;
    setSelectedPlan(0);
    setSelectedLesson(0);
    setSelectedExercise(0);
  }, []);

  const activePlan = learningPlans[selectedPlan] ?? {
    accent: 'blue',
    title: 'No learning plans yet',
    language: targetLanguage,
    progress: 0,
    lessons: [],
    goal: profile.learningGoal,
    summary: 'Create a plan in the backend to start learning.',
    duration: '--',
  };
  const activeLesson = activePlan.lessons[selectedLesson] ?? {
    id: null,
    accent: 'blue',
    icon: null,
    title: 'No lesson selected',
    topic: 'Choose a lesson from your learning plans.',
    status: 'not-started',
    progress: 0,
    exercises: [],
    blocks: [],
  };
  const activeExercise = activeLesson.exercises[selectedExercise] ?? {
    id: null,
    type: 'reading',
    title: 'No exercise selected',
    status: 'not-started',
    format: 'Short written answer',
    task: 'Open a lesson exercise to start practicing.',
    prompt: '',
    expectedAnswer: '',
    keywords: [],
    feedback: null,
    answerText: '',
  };
  const recentLessons = getRecentLessons(learningPlans);
  const recentFinishedLessons = progress.recentFinished.length > 0 ? progress.recentFinished : getRecentFinishedLessons(learningPlans);
  const t = translations[language] ?? translations.English;

  const syncLearningData = React.useCallback(async (currentSession, options = {}) => {
    setDashboardError('');
    setProfileError('');

    const token = currentSession.accessToken;
    const userId = currentSession.user.id;
    const profileResult = await getUserProfile(userId, token)
      .then((value) => ({ status: 'fulfilled', value }))
      .catch((reason) => ({ status: 'rejected', reason }));

    const nextProfile = profileResult.status === 'fulfilled'
      ? toProfile(profileResult.value, currentSession.user)
      : toProfile(null, currentSession.user);
    const contentLanguage = nextProfile.targetLanguage || targetLanguageRef.current;
    let learningPlansError = null;
    let nextLearningPlans = [];

    try {
      nextLearningPlans = toLearningPlans(await getLearningPlans(userId, token, contentLanguage));
    } catch (error) {
      learningPlansError = error;
    }

    if (
      learningPlansError?.status === 404
      || (!learningPlansError && nextLearningPlans.length === 0)
    ) {
      try {
        await createDefaultLearningPlan({
          user_id: userId,
          target_language: nextProfile.targetLanguage,
          current_level: nextProfile.currentLevel,
          learning_goal: nextProfile.learningGoal,
        }, token);
        nextLearningPlans = toLearningPlans(await getLearningPlans(userId, token, contentLanguage));
        learningPlansError = null;
      } catch (error) {
        learningPlansError = error;
      }
    }
    if (nextLearningPlans.length > 0) {
      try {
        const savedAnswers = await getUserAnswers(userId, token, { targetLanguage: contentLanguage });
        const lessonDetails = await Promise.all(
          nextLearningPlans.flatMap((plan) => plan.lessons.map(async (lesson) => {
            const lessonResponse = await getLesson(lesson.id, token, contentLanguage);
            return toLessonDetail(lessonResponse, lesson);
          })),
        );

        nextLearningPlans = derivePlanProgress(
          lessonDetails.reduce(
            (plans, lessonDetail) => mergeLessonIntoPlans(
              plans,
              attachSavedAnswersToLesson(lessonDetail, savedAnswers),
            ),
            nextLearningPlans,
          ),
        );
      } catch (error) {
        if (error.status !== 404) {
          setDashboardError(error.message || 'Unable to load lesson progress.');
        }
        nextLearningPlans = derivePlanProgress(nextLearningPlans);
      }
    }

    const progressPlanId = options.progressPlanId
      ?? nextLearningPlans[selectedPlanRef.current]?.id
      ?? nextLearningPlans[0]?.id;
    const progressResult = await getProgress(userId, token, {
      planId: progressPlanId,
      targetLanguage: contentLanguage,
    })
      .then((value) => ({ status: 'fulfilled', value }))
      .catch((reason) => ({ status: 'rejected', reason }));

    const nextProgress = progressResult.status === 'fulfilled'
      ? toProgressSummary(progressResult.value, nextLearningPlans)
      : createEmptyProgress(userId);

    setProfile(nextProfile);
    setTargetLanguageValue(nextProfile.targetLanguage || targetLanguageRef.current);
    setLearningPlans(nextLearningPlans);
    setProgress(nextProgress);

    if (!options.skipSelectionReset) {
      resetLearningSelection();
    }

    if (profileResult.status === 'rejected' && profileResult.reason?.status !== 404) {
      setProfileError(profileResult.reason.message);
    }

    if (learningPlansError) {
      setDashboardError(learningPlansError.message);
    }

    if (progressResult.status === 'rejected' && progressResult.reason?.status !== 404) {
      setDashboardError(progressResult.reason.message);
    }

    return { nextLearningPlans, nextProgress };
  }, [resetLearningSelection, setTargetLanguageValue]);

  React.useEffect(() => {
    if (!authEnabled) {
      return;
    }

    let cancelled = false;
    let introTimeoutId = 0;

    async function bootstrapOidcSession() {
      setAuthPending(true);
      setAuthError('');
      setLoadingInitialData(true);

      try {
        const authenticated = await initializeAuth();
        if (!authenticated || cancelled) {
          return;
        }

        const accessToken = await getValidAccessToken();
        if (!accessToken) {
          return;
        }

        const user = await getCurrentUser(accessToken);
        if (cancelled) {
          return;
        }

        const nextSession = {
          user,
          accessToken,
          tokenType: 'Bearer',
        };

        setSession(nextSession);
        await syncLearningData(nextSession);
        if (!cancelled && !initialAuthRedirectDoneRef.current && screenRef.current === 'auth') {
          initialAuthRedirectDoneRef.current = true;
          navigateToScreen('main');
          setSettingsOpen(false);
          setSettingsClosing(false);
          if (window.localStorage.getItem(introPendingKey) === 'true') {
            window.localStorage.removeItem(introPendingKey);
            introTimeoutId = window.setTimeout(() => {
              if (!cancelled) {
                setIntroOpen(true);
              }
            }, 260);
          }
        }
      } catch (error) {
        if (!cancelled) {
          setAuthError(error.message || 'Unable to complete Keycloak sign in.');
        }
      } finally {
        if (!cancelled) {
          setAuthPending(false);
          setLoadingInitialData(false);
        }
      }
    }

    bootstrapOidcSession();

    return () => {
      cancelled = true;
      window.clearTimeout(introTimeoutId);
    };
  }, [navigateToScreen, syncLearningData]);

  const loadLessonDetail = React.useCallback(async (planIndex, lessonIndex, currentSession) => {
    const selectedPlanSummary = learningPlans[planIndex];
    const selectedLessonSummary = selectedPlanSummary?.lessons[lessonIndex];

    if (!selectedLessonSummary?.id) {
      return;
    }

    const lessonResponse = await getLesson(
      selectedLessonSummary.id,
      currentSession.accessToken,
      profile.targetLanguage,
    );
    const lessonDetail = toLessonDetail(lessonResponse, selectedLessonSummary);
    const lessonExerciseIds = new Set(lessonDetail.exercises.map((exercise) => exercise.id));
    const savedAnswers = (await getUserAnswers(currentSession.user.id, currentSession.accessToken, {
      planId: selectedPlanSummary.id,
      targetLanguage: profile.targetLanguage,
    }))
      .filter((answer) => lessonExerciseIds.has(answer.exercise_id));
    const feedbackEntries = await Promise.all(
      savedAnswers.map(async (answer) => {
        try {
          return [answer.id, await getFeedbackByAnswerId(answer.id, currentSession.accessToken)];
        } catch (error) {
          if (error.status === 404) {
            return [answer.id, null];
          }

          throw error;
        }
      }),
    );
    const feedbackByAnswerId = new Map(
      feedbackEntries.filter(([, feedback]) => feedback),
    );
    const normalizedLesson = attachSavedAnswersToLesson(
      lessonDetail,
      savedAnswers,
      feedbackByAnswerId,
    );

    setLearningPlans((currentPlans) => mergeLessonIntoPlans(currentPlans, normalizedLesson));
  }, [learningPlans, profile.targetLanguage]);

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
        navigateToScreen(nextScreen);
      }
    }, 220);
  };

  const goToMain = () => {
    navigateToScreen('main');
    setSettingsOpen(false);
    setSettingsClosing(false);
    setIntroOpen(false);
  };

  const bypassAuth = async () => {
    setAuthError('');
    setDashboardError('');
    setProfileError('');
    setLearningError('');
    setLoadingInitialData(true);

    try {
      const user = await getCurrentUser();
      const nextSession = {
        user,
        accessToken: null,
        tokenType: 'None',
      };

      setSession(nextSession);
      await syncLearningData(nextSession);
      initialAuthRedirectDoneRef.current = true;
      navigateToScreen('main');
    } catch (error) {
      setAuthError(error.message || 'Unable to continue without authentication.');
    } finally {
      setLoadingInitialData(false);
    }
  };

  const resetSessionState = () => {
    setSession(null);
    setAuthError('');
    setDashboardError('');
    setProfileError('');
    setAnswerError('');
    setLearningError('');
    setProfile(defaultProfile);
    setTargetLanguageValue(defaultProfile.targetLanguage);
    setLearningPlans([]);
    setProgress(createEmptyProgress());
    resetLearningSelection();
    initialAuthRedirectDoneRef.current = false;
    navigateToScreen('auth');
    setSettingsOpen(false);
    setSettingsClosing(false);
  };

  const logout = () => {
    resetSessionState();
    if (authEnabled) {
      logoutFromKeycloak().catch(() => {
        setAuthError('Unable to complete Keycloak logout.');
      });
    }
  };

  const applyProfileState = React.useCallback((nextProfile) => {
    setProfile(nextProfile);
    setTargetLanguageValue(nextProfile.targetLanguage);
  }, [setTargetLanguageValue]);

  const handleLogin = () => {
    setAuthError('');
    setAuthPending(true);
    loginWithKeycloak().catch((error) => {
      setAuthPending(false);
      setAuthError(error.message || 'Unable to start Keycloak sign in.');
    });
  };

  const handleRegister = () => {
    setAuthError('');
    setAuthPending(true);
    window.localStorage.setItem(introPendingKey, 'true');
    registerWithKeycloak().catch((error) => {
      setAuthPending(false);
      window.localStorage.removeItem(introPendingKey);
      setAuthError(error.message || 'Unable to start Keycloak registration.');
    });
  };

  const finishIntro = () => {
    window.localStorage.removeItem(introPendingKey);
    setIntroOpen(false);
  };

  const openLessonFromDashboard = (planIndex, lessonIndex) => {
    setLearningError('');
    selectPlan(planIndex);
    selectLesson(lessonIndex);
    selectExercise(0);
    setLearningMode('training');
    setLearningStep('lesson');
    navigateToScreen('learn');

    if (session) {
      loadLessonDetail(planIndex, lessonIndex, session).catch((error) => {
        setLearningError(error.message || 'Unable to load lesson.');
      });
    }
  };

  const openPlanFromDashboard = () => {
    if (learningPlans.length === 0) {
      setLearningMode('training');
      setLearningStep('plans');
      navigateToScreen('learn');
      return;
    }

    selectLesson(0);
    selectExercise(0);
    setLearningMode('training');
    setLearningStep('lessons');
    navigateToScreen('learn');
  };

  const openPlan = (planIndex) => {
    selectPlan(planIndex);
    selectLesson(0);
    selectExercise(0);
    setLearningStep('lessons');
  };

  const openLesson = (lessonIndex) => {
    setLearningError('');
    selectLesson(lessonIndex);
    selectExercise(0);
    setLearningStep('lesson');

    if (session) {
      loadLessonDetail(selectedPlanRef.current, lessonIndex, session).catch((error) => {
        setLearningError(error.message || 'Unable to load lesson.');
      });
    }
  };

  const openExercise = (exerciseIndex) => {
    setAnswerError('');
    selectExercise(exerciseIndex);
    setLearningStep('exercise');

    const exercise = activeLesson.exercises[exerciseIndex];
    if (exercise?.type === 'listening' && exercise?.id && activeLesson?.id) {
      const requestId = ++listeningRequestIdRef.current;
      setListeningContent(null);
      setListeningSelections({});
      setListeningError('');

      if (isReviewedListeningExercise(exercise)) {
        setListeningLoading(false);
        return;
      }

      setListeningLoading(true);
      generateListeningContent(
        exercise.id,
        activeLesson.id,
        profile.targetLanguage,
        profile.currentLevel,
        session?.accessToken,
      ).then((content) => {
        if (listeningRequestIdRef.current === requestId) setListeningContent(content);
      }).catch((error) => {
        if (listeningRequestIdRef.current === requestId)
          setListeningError(error.message || 'Unable to load listening exercise.');
      }).finally(() => {
        if (listeningRequestIdRef.current === requestId) setListeningLoading(false);
      });
    }
  };

  const handleRetryListeningExercise = async (exercise) => {
    const currentExercise = activeLesson.exercises[selectedExercise];

    if (
      !exercise?.id
      || !activeLesson?.id
      || currentExercise?.id !== exercise.id
      || !isReviewedListeningExercise(currentExercise)
    ) {
      setListeningError('Open the reviewed listening exercise before retrying.');
      return;
    }

    const requestId = ++listeningRequestIdRef.current;
    setListeningContent(null);
    setListeningSelections({});
    setListeningError('');
    setListeningLoading(true);

    try {
      const content = await generateListeningContent(
        exercise.id,
        activeLesson.id,
        profile.targetLanguage,
        profile.currentLevel,
        session?.accessToken,
      );

      if (listeningRequestIdRef.current === requestId) {
        setListeningContent(content);
        return content;
      }

      return null;
    } catch (error) {
      if (listeningRequestIdRef.current === requestId) {
        setListeningError(error.message || 'Unable to load listening exercise.');
      }

      return null;
    } finally {
      if (listeningRequestIdRef.current === requestId) {
        setListeningLoading(false);
      }
    }
  };

  const changeLearningMode = (nextMode) => {
    setLearningMode(nextMode);
    setLearningStep('plans');
  };

  const handleProfileChange = async (nextProfile) => {
    const resolvedProfile = {
      ...profile,
      ...nextProfile,
    };

    if (!session) {
      setProfileError('Please sign in before updating your profile.');
      return;
    }

    setProfilePending(true);
    setProfileError('');

    try {
      const savedProfile = await updateUserProfile(session.user.id, {
        name: resolvedProfile.name,
        country: resolvedProfile.country,
        target_language: resolvedProfile.targetLanguage,
        current_level: resolvedProfile.currentLevel,
        learning_goal: resolvedProfile.learningGoal,
      }, session.accessToken);
      const normalizedProfile = toProfile(savedProfile, session.user);
      applyProfileState(normalizedProfile);
    } catch (error) {
      setProfileError(error.message || 'Unable to save profile.');
      throw error;
    } finally {
      setProfilePending(false);
    }
  };

  const handleTargetLanguageChange = async (nextTargetLanguage) => {
    if (nextTargetLanguage === targetLanguage || switchingTargetLanguage) {
      return;
    }

    if (!session) {
      setTargetLanguageValue(nextTargetLanguage);
      setProfile((currentProfile) => ({
        ...currentProfile,
        targetLanguage: nextTargetLanguage,
      }));
      return;
    }

    setSwitchingTargetLanguage(nextTargetLanguage);
    setProfilePending(true);
    setProfileError('');

    const resolvedProfile = {
      ...profile,
      targetLanguage: nextTargetLanguage,
    };

    try {
      const savedProfile = await updateUserProfile(session.user.id, {
        name: resolvedProfile.name,
        country: resolvedProfile.country,
        target_language: nextTargetLanguage,
        current_level: resolvedProfile.currentLevel,
        learning_goal: resolvedProfile.learningGoal,
      }, session.accessToken);
      const normalizedProfile = toProfile(savedProfile, session.user);
      applyProfileState(normalizedProfile);
      await syncLearningData(session, { skipSelectionReset: true });
    } catch (error) {
      setProfileError(error.message || 'Unable to switch learning language.');
    } finally {
      setProfilePending(false);
      setSwitchingTargetLanguage('');
    }
  };

  const handleSubmitAnswer = async (exercise, answerText) => {
    if (!session || !exercise?.id || !activeLesson?.id || !activePlan?.id) {
      setAnswerError('Please sign in and open a lesson before submitting an answer.');
      return;
    }

    setAnswerPending(true);
    setAnswerError('');

    const resolvedAnswerText = exercise.type === 'listening'
      ? JSON.stringify(listeningSelections)
      : answerText;

    try {
      const submission = await submitAnswer({
        user_id: session.user.id,
        exercise_id: exercise.id,
        answer_text: resolvedAnswerText,
        client_context: {
          lesson_id: activeLesson.id,
          plan_id: activePlan.id,
          target_language: profile.targetLanguage,
          level: profile.currentLevel,
        },
      }, session.accessToken);

      const lessonResponse = await getLesson(activeLesson.id, session.accessToken, profile.targetLanguage);
      const normalizedLesson = attachSubmissionToLesson(
        toLessonDetail(lessonResponse, activeLesson),
        submission,
      );
      const syncResult = await syncLearningData(session, {
        skipSelectionReset: true,
        progressPlanId: activePlan.id,
      });
      setLearningPlans(mergeLessonIntoPlans(syncResult.nextLearningPlans, normalizedLesson));
    } catch (error) {
      setAnswerError(error.message || 'Unable to submit answer.');
    } finally {
      setAnswerPending(false);
    }
  };

  const handleSubmitSpeakingAnswer = async (exercise, audio) => {
    if (!session) {
      setAnswerError('Please sign in before submitting a speaking answer.');
      return;
    }

    if (!exercise?.id || !activeLesson?.id || !activePlan?.id) {
      setAnswerError('Open a speaking exercise from a lesson before submitting audio.');
      return;
    }

    if (!audio) {
      setAnswerError('Record or select an audio file before submitting.');
      return;
    }

    setAnswerPending(true);
    setAnswerError('');

    try {
      const submission = await submitSpeakingAnswer({
        audio,
        user_id: session.user.id,
        exercise_id: exercise.id,
        lesson_id: activeLesson.id,
        plan_id: activePlan.id,
        target_language: profile.targetLanguage,
        level: profile.currentLevel || exercise.difficulty,
      }, session.accessToken);

      const lessonResponse = await getLesson(
        activeLesson.id,
        session.accessToken,
        profile.targetLanguage,
      );
      const normalizedLesson = attachSubmissionToLesson(
        toLessonDetail(lessonResponse, activeLesson),
        submission,
      );
      const syncResult = await syncLearningData(session, {
        skipSelectionReset: true,
        progressPlanId: activePlan.id,
      });
      setLearningPlans(mergeLessonIntoPlans(syncResult.nextLearningPlans, normalizedLesson));
    } catch (error) {
      setAnswerError(error.message || 'Unable to submit speaking answer.');
    } finally {
      setAnswerPending(false);
    }
  };

  const handleCreateAiLearningPlan = async (payload) => {
    if (!session) {
      throw new Error('Please sign in before generating a learning plan.');
    }

    const createdPlan = await createAiLearningPlan({
      ...payload,
      user_id: session.user.id,
      target_language: profile.targetLanguage,
      current_level: profile.currentLevel,
    }, session.accessToken);

    const syncResult = await syncLearningData(session, { skipSelectionReset: true });
    const createdPlanIndex = syncResult.nextLearningPlans.findIndex((plan) => plan.id === createdPlan.id);
    setSelectedPlan(createdPlanIndex >= 0 ? createdPlanIndex : 0);
    setSelectedLesson(0);
    setSelectedExercise(0);
    setLearningMode('training');
    setLearningStep('lessons');

    return createdPlan;
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

        <section className={`app-shell ${darkMode ? 'dark-mode' : ''}`} aria-label="InterviewMate">
          <div className="status-bar" aria-hidden="true">
            <span>9:41</span>
            <span>LTE 100%</span>
          </div>

          {screen === 'auth' && (
            <AuthPage
              authEnabled={authEnabled}
              authErrorMessage={authError}
              authPending={authPending}
              t={t}
              onLogin={handleLogin}
              onRegister={handleRegister}
              onBypass={bypassAuth}
            />
          )}

          {screen === 'main' && (
            <DashboardPage
              activePlan={activePlan}
              dashboardError={dashboardError}
              hasLearningPlans={learningPlans.length > 0}
              hasLessons={activePlan.lessons.length > 0}
              recentFinishedLessons={recentFinishedLessons}
              recentLessons={recentLessons}
              profile={profile}
              t={t}
              onNavigate={navigateToScreen}
              onOpenLearningHub={() => {
                setLearningMode('training');
                setLearningStep('plans');
                navigateToScreen('learn');
              }}
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
              answerError={answerError}
              answerPending={answerPending}
              learningError={learningError}
              learningMode={learningMode}
              learningPlans={learningPlans}
              learningStep={learningStep}
              listeningContent={listeningContent}
              listeningError={listeningError}
              listeningLoading={listeningLoading}
              listeningSelections={listeningSelections}
              profile={profile}
              t={t}
              onBack={goBackInLearning}
              onLearningMode={changeLearningMode}
              onNavigate={navigateToScreen}
              onOpenExercise={openExercise}
              onOpenLesson={openLesson}
              onOpenPlan={openPlan}
              onOpenPlanDetail={() => setLearningStep('planDetail')}
              onOpenSettings={openSettings}
              onListeningSelect={(qIndex, optionText) =>
                setListeningSelections((prev) => ({ ...prev, [qIndex]: optionText }))
              }
              onRetryListeningExercise={handleRetryListeningExercise}
              onSubmitAnswer={handleSubmitAnswer}
              onCreateAiLearningPlan={handleCreateAiLearningPlan}
              onSubmitSpeakingAnswer={handleSubmitSpeakingAnswer}
            />
          )}

          {screen === 'profile' && (
            <ProfilePage
              profile={profile}
              profileError={profileError}
              profilePending={profilePending}
              t={t}
              onBack={goToMain}
              onProfileChange={handleProfileChange}
            />
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
              pending={Boolean(switchingTargetLanguage)}
              title={t.languageToLearn}
              onBack={goToMain}
              onLanguage={handleTargetLanguageChange}
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
              onLogout={logout}
              onProfile={() => closeSettings('profile')}
              onToggleDarkMode={() => setDarkMode((value) => !value)}
            />
          )}

          {introOpen && screen === 'main' && !settingsOpen && (
            <IntroOverlay onFinish={finishIntro} />
          )}

          {switchingTargetLanguage && (
            <LanguageSwitchOverlay targetLanguage={switchingTargetLanguage} t={t} />
          )}

          <div className="home-indicator" aria-hidden="true"></div>
        </section>
      </div>
    </main>
  );
}
