import {
  BookOpen,
  BriefcaseBusiness,
  CheckCircle2,
  GraduationCap,
  Languages,
  MapPinned,
  MessageSquareText,
  Presentation,
  Sparkles,
  Target,
  TextCursorInput,
} from 'lucide-react';

const planAccents = ['blue', 'green', 'amber', 'violet', 'rose'];
const lessonAccents = ['blue', 'green', 'amber', 'violet', 'rose'];
const lessonIcons = [
  MessageSquareText,
  GraduationCap,
  BriefcaseBusiness,
  Presentation,
  Sparkles,
  Languages,
  MapPinned,
  BookOpen,
];

function pickAccent(index, palette) {
  return palette[index % palette.length];
}

function pickLessonIcon(index) {
  return lessonIcons[index % lessonIcons.length];
}

function formatPlanSummary(plan) {
  return plan.description || plan.goal || 'Guided language learning plan.';
}

function clampProgress(progress) {
  return Math.max(0, Math.min(100, Math.round(progress ?? 0)));
}

function progressStatus(progress, fallbackStatus) {
  if (progress >= 100) {
    return 'finished';
  }

  if (progress > 0) {
    return 'ongoing';
  }

  return fallbackStatus ?? 'not-started';
}

function withDerivedLessonProgress(lesson) {
  const totalExercises = lesson.exerciseCount || lesson.exercises.length;

  if (totalExercises === 0) {
    const progress = clampProgress(lesson.progress);
    return {
      ...lesson,
      progress,
      status: progressStatus(progress, lesson.status),
    };
  }

  const completedExerciseIds = new Set(
    lesson.exercises
      .filter((exercise) => exercise.status === 'finished' || exercise.score != null || exercise.answerText)
      .map((exercise) => exercise.id),
  );
  const progress = clampProgress((completedExerciseIds.size / totalExercises) * 100);

  return {
    ...lesson,
    progress,
    status: progressStatus(progress, lesson.status),
  };
}

function withDerivedPlanProgress(plan) {
  if (plan.lessons.length === 0) {
    const progress = clampProgress(plan.progress);
    return {
      ...plan,
      progress,
      status: progressStatus(progress, plan.status),
    };
  }

  const progress = clampProgress(
    plan.lessons.reduce((total, lesson) => total + (lesson.progress ?? 0), 0) / plan.lessons.length,
  );

  return {
    ...plan,
    progress,
    status: progressStatus(progress, plan.status),
  };
}

function fallbackBlocks(lesson) {
  return [
    {
      type: 'content',
      title: 'Core idea',
      subtitle: `${lesson.timeEstimateMinutes ?? 10} min`,
      text: lesson.topic,
      points: [
        'Review the task before answering.',
        'Use the lesson exercises to practice the target structure.',
      ],
    },
    ...lesson.exercises.map((exercise, index) => ({
      type: 'exercise',
      exerciseIndex: index,
    })),
  ];
}

function exerciseBlock(index) {
  return {
    type: 'exercise',
    exerciseIndex: index,
  };
}

function contentBlockTitle(block, index) {
  const title = block.title?.trim();
  if (title && title.toLowerCase() !== 'lesson content') {
    return title;
  }

  return `Learning note ${index + 1}`;
}

function hasBlankMarker(value = '') {
  return value.includes('___') || value.includes('____') || value.toLowerCase().includes('[blank]');
}

function formatExercise(subtype, fallbackFormat) {
  switch (subtype) {
    case 'translation':
      return 'Translation';
    case 'fill_in_blank':
      return 'Fill in the blank';
    case 'sentence_building':
      return 'Sentence building';
    case 'speaking_prompt':
      return 'Spoken response';
    case 'listening_choice':
      return 'Listening choice';
    case 'multiple_choice':
      return 'Multiple choice';
    case 'free_text':
      return 'Short written answer';
    default:
      return fallbackFormat ?? 'Practice';
  }
}

function normalizeExercisePrompt(exercise) {
  const question = exercise.question || exercise.title || '';
  const expectedAnswer = exercise.expected_answer ?? '';
  const subtype = exercise.subtype === 'fill_in_blank' && !hasBlankMarker(question)
    ? 'free_text'
    : exercise.subtype;
  const normalizedQuestion = exercise.subtype === 'fill_in_blank' && subtype === 'free_text' && expectedAnswer
    ? `Write a short German answer using these terms: ${expectedAnswer}.`
    : question;

  return {
    question: normalizedQuestion,
    subtype,
    format: formatExercise(subtype, exercise.format),
  };
}

export function createEmptyProgress(userId = null) {
  return {
    userId,
    completedExercises: 0,
    totalExercises: 0,
    averageScore: 0,
    recentProgress: [],
    recentFinished: [],
    topLessons: [],
    currentPlan: null,
  };
}

export function toProfile(profile, fallbackUser) {
  return {
    id: profile?.id ?? null,
    userId: profile?.user_id ?? fallbackUser?.id ?? null,
    name: profile?.name ?? fallbackUser?.name ?? '',
    country: profile?.country ?? 'Unknown',
    targetLanguage: profile?.target_language ?? 'German',
    currentLevel: profile?.current_level ?? 'A2',
    learningGoal: profile?.learning_goal ?? 'Prepare for a software engineering job interview',
    email: fallbackUser?.email ?? '',
  };
}

export function toLearningPlans(plans = []) {
  return plans.map((plan, planIndex) => ({
    id: plan.id,
    userId: plan.user_id,
    title: plan.title,
    description: plan.description,
    goal: plan.goal,
    language: plan.language,
    level: plan.level,
    duration: plan.duration,
    status: plan.status,
    progress: plan.progress ?? 0,
    summary: formatPlanSummary(plan),
    accent: pickAccent(planIndex, planAccents),
    lessons: (plan.lessons ?? []).map((lesson, lessonIndex) => ({
      id: lesson.id,
      planId: lesson.plan_id,
      title: lesson.title,
      topic: lesson.topic,
      orderNumber: lesson.order_number,
      status: lesson.status,
      progress: lesson.progress ?? 0,
      timeEstimateMinutes: lesson.time_estimate_minutes ?? 10,
      exerciseCount: lesson.exercise_count ?? 0,
      accent: pickAccent(lessonIndex, lessonAccents),
      icon: pickLessonIcon(lessonIndex),
      exercises: [],
      blocks: [],
      comment: null,
    })),
  }));
}

export function toLessonDetail(lesson, planSummaryLesson) {
  const exercises = (lesson.exercises ?? []).map((exercise) => {
    const normalizedExercise = normalizeExercisePrompt(exercise);

    return {
      id: exercise.id,
      lessonId: exercise.lesson_id,
      type: exercise.type,
      subtype: normalizedExercise.subtype,
      title: normalizedExercise.question || exercise.title,
      question: normalizedExercise.question,
      expectedAnswer: exercise.expected_answer,
      difficulty: exercise.difficulty,
      status: exercise.status,
      score: exercise.score,
      format: normalizedExercise.format,
      source: exercise.source,
      grade: exercise.score,
      task: normalizedExercise.question,
      prompt: normalizedExercise.question,
      feedback: null,
      answerText: '',
    };
  });

  const exerciseIndexById = new Map(exercises.map((exercise, index) => [exercise.id, index]));
  const exerciseIndexesInBlocks = new Set();
  const blocks = (lesson.content_blocks ?? []).map((block, blockIndex) => {
    if (block.type === 'exercise') {
      const exerciseIndex = exerciseIndexById.get(block.exercise_id);
      if (exerciseIndex === undefined) {
        return null;
      }

      exerciseIndexesInBlocks.add(exerciseIndex);
      return exerciseBlock(exerciseIndex);
    }

    return {
      type: block.type,
      title: contentBlockTitle(block, blockIndex),
      subtitle: block.subtitle ?? `${lesson.time_estimate_minutes ?? 10} min`,
      text: block.text ?? lesson.topic,
      points: block.points ?? [],
    };
  }).filter(Boolean);
  const missingExerciseBlocks = exercises
    .map((_, index) => index)
    .filter((index) => !exerciseIndexesInBlocks.has(index))
    .map(exerciseBlock);
  const lessonBlocks = [...blocks, ...missingExerciseBlocks];

  const normalizedLesson = {
    id: lesson.id,
    planId: lesson.plan_id,
    title: lesson.title,
    topic: lesson.topic,
    orderNumber: lesson.order_number,
    status: lesson.status,
    progress: lesson.progress ?? 0,
    timeEstimateMinutes: lesson.time_estimate_minutes ?? planSummaryLesson?.timeEstimateMinutes ?? 10,
    exerciseCount: lesson.exercise_count ?? exercises.length,
    accent: planSummaryLesson?.accent ?? pickAccent((lesson.order_number ?? 1) - 1, lessonAccents),
    icon: planSummaryLesson?.icon ?? pickLessonIcon((lesson.order_number ?? 1) - 1),
    exercises,
    blocks: lessonBlocks.length > 0 ? lessonBlocks : fallbackBlocks({
      topic: lesson.topic,
      exercises,
      timeEstimateMinutes: lesson.time_estimate_minutes,
    }),
    comment: lesson.status === 'finished' ? 'Great work. This lesson is complete.' : null,
  };

  return normalizedLesson;
}

export function mergeLessonIntoPlans(plans, lessonDetail) {
  return plans.map((plan) => {
    if (plan.id !== lessonDetail.planId) {
      return plan;
    }

    return withDerivedPlanProgress({
      ...plan,
      lessons: plan.lessons.map((lesson) => (
        lesson.id === lessonDetail.id ? withDerivedLessonProgress({ ...lesson, ...lessonDetail }) : lesson
      )),
    });
  });
}

export function toProgressSummary(progress, learningPlans) {
  if (!progress) {
    return createEmptyProgress();
  }

  const lessonLookup = buildLessonLookup(learningPlans);

  function withLessonSelection(item, fallbackAccentIndex) {
    const selection = lessonLookup.get(item.lesson_id) ?? {};
    return {
      planId: item.plan_id,
      lessonId: item.lesson_id,
      planIndex: selection.planIndex ?? 0,
      lessonIndex: selection.lessonIndex ?? 0,
      title: item.lesson_title,
      planTitle: learningPlans.find((plan) => plan.id === item.plan_id)?.title ?? 'Learning plan',
      status: item.status,
      progress: item.progress ?? item.score ?? 0,
      accent: pickAccent(fallbackAccentIndex, lessonAccents),
    };
  }

  return {
    userId: progress.user_id,
    completedExercises: progress.completed_exercises ?? 0,
    totalExercises: progress.total_exercises ?? 0,
    averageScore: progress.average_score ?? 0,
    currentPlan: progress.current_plan ?? learningPlans[0] ?? null,
    recentProgress: (progress.recent_progress ?? []).map((item) => withLessonSelection(item, 0)),
    recentFinished: (progress.recent_finished ?? []).map((item) => withLessonSelection({
      ...item,
      status: 'finished',
      progress: item.score ?? 100,
    }, 1)),
    topLessons: progress.top_lessons ?? [],
  };
}

export function attachSubmissionToLesson(lesson, submission) {
  if (!lesson || !submission?.answer) {
    return lesson;
  }

  const transcription = submission.transcription ?? submission.answer.answer_text;

  return withDerivedLessonProgress({
    ...lesson,
    progress: submission.lesson_progress ?? lesson.progress,
    exercises: lesson.exercises.map((exercise) => {
      if (exercise.id !== submission.answer.exercise_id) {
        return exercise;
      }

      return {
        ...exercise,
        status: submission.exercise_status ?? exercise.status,
        score: submission.answer.score,
        grade: submission.answer.score,
        answerText: transcription,
        feedback: submission.feedback
          ? {
              title: 'AI feedback',
              score: submission.answer.score,
              message: submission.feedback.message,
              weakArea: submission.feedback.weak_area,
              transcription,
              feedbackAudioB64: submission.feedback_audio_b64,
              strengths: submission.feedback.strengths ?? [],
              improvements: submission.feedback.improvements
                ?? (submission.feedback.weak_area ? [`Focus on: ${submission.feedback.weak_area}`] : []),
              improvedExample: submission.feedback.corrected_answer,
            }
        : null,
      };
    }),
  });
}

export function attachSavedAnswersToLesson(lesson, savedAnswers = [], feedbackByAnswerId = new Map()) {
  if (!lesson || savedAnswers.length === 0) {
    return withDerivedLessonProgress(lesson);
  }

  const answerByExerciseId = new Map(
    savedAnswers.map((answer) => [answer.exercise_id, answer]),
  );

  return withDerivedLessonProgress({
    ...lesson,
    exercises: lesson.exercises.map((exercise) => {
      const answer = answerByExerciseId.get(exercise.id);

      if (!answer) {
        return exercise;
      }

      const feedback = feedbackByAnswerId.get(answer.id);

      return {
        ...exercise,
        status: 'finished',
        score: answer.score,
        grade: answer.score,
        answerText: answer.answer_text,
        feedback: feedback
          ? {
              title: 'AI feedback',
              score: answer.score,
              message: feedback.message,
              weakArea: feedback.weak_area,
              transcription: answer.answer_text,
              strengths: feedback.strengths ?? [],
              improvements: feedback.improvements
                ?? (feedback.weak_area ? [`Focus on: ${feedback.weak_area}`] : []),
              improvedExample: feedback.corrected_answer,
            }
        : exercise.feedback,
      };
    }),
  });
}

export function derivePlanProgress(plans) {
  return plans.map((plan) => withDerivedPlanProgress({
    ...plan,
    lessons: plan.lessons.map(withDerivedLessonProgress),
  }));
}

export function buildLessonLookup(learningPlans) {
  const lessonMap = new Map();

  learningPlans.forEach((plan, planIndex) => {
    plan.lessons.forEach((lesson, lessonIndex) => {
      lessonMap.set(lesson.id, { planIndex, lessonIndex });
    });
  });

  return lessonMap;
}
