import { describe, expect, it } from 'vitest';
import {
  attachSavedAnswersToLesson,
  attachSubmissionToLesson,
  derivePlanProgress,
  toLearningPlans,
  toLessonDetail,
  toProgressSummary,
} from './mappers';

describe('frontend data mapper unit behavior', () => {
  it('normalizes backend learning plans into UI-ready plans and lessons', () => {
    const plans = toLearningPlans([
      {
        id: 10,
        user_id: 2,
        title: 'Interview German',
        description: 'Prepare professional answers.',
        language: 'German',
        level: 'A2',
        duration: '2 weeks',
        status: 'ongoing',
        lessons: [
          {
            id: 20,
            plan_id: 10,
            title: 'Self introduction',
            topic: 'Introduce yourself clearly.',
            order_number: 1,
            status: 'not-started',
            progress: 0,
            time_estimate_minutes: 12,
            exercise_count: 2,
          },
        ],
      },
    ]);

    expect(plans[0]).toMatchObject({
      id: 10,
      userId: 2,
      title: 'Interview German',
      summary: 'Prepare professional answers.',
      accent: 'blue',
      origin: 'FIXED',
    });
    expect(plans[0].lessons[0]).toMatchObject({
      id: 20,
      planId: 10,
      timeEstimateMinutes: 12,
      exerciseCount: 2,
      language: 'German',
      exercises: [],
      blocks: [],
    });
  });

  it('builds fallback lesson blocks and derives progress from saved answers', () => {
    const lesson = toLessonDetail(
      {
        id: 20,
        plan_id: 10,
        title: 'Self introduction',
        topic: 'Introduce yourself clearly.',
        order_number: 1,
        exercises: [
          {
            id: 100,
            lesson_id: 20,
            type: 'writing',
            question: 'Write your introduction.',
            expected_answer: 'A concise professional introduction.',
            difficulty: 'A2',
          },
          {
            id: 101,
            lesson_id: 20,
            type: 'speaking',
            question: 'Say your introduction.',
            expected_answer: 'A clear spoken introduction.',
            difficulty: 'A2',
          },
        ],
        content_blocks: [
          {
            type: 'content',
            title: 'Kernidee',
            subtitle: '12 min',
            text: 'Introduce yourself clearly.',
          },
          { type: 'exercise', exercise_id: 100 },
          { type: 'exercise', exercise_id: 101 },
        ],
      },
      { language: 'German', accent: 'blue' },
    );

    const withSavedAnswer = attachSavedAnswersToLesson(
      lesson,
      [{ id: 500, exercise_id: 100, answer_text: 'Hallo, ich bin Alex.', score: 82 }],
      new Map([[500, { message: 'Good structure.', weak_area: 'Detail', strengths: ['Clear'], corrected_answer: 'Hallo, ich bin Alex...' }]]),
    );

    expect(withSavedAnswer.blocks).toEqual([
      expect.objectContaining({ type: 'content', title: 'Kernidee' }),
      expect.objectContaining({ type: 'exercise', exerciseIndex: 0 }),
      expect.objectContaining({ type: 'exercise', exerciseIndex: 1 }),
    ]);
    expect(withSavedAnswer.progress).toBe(50);
    expect(withSavedAnswer.status).toBe('ongoing');
    expect(withSavedAnswer.exercises[0]).toMatchObject({
      status: 'finished',
      score: 82,
      answerText: 'Hallo, ich bin Alex.',
      feedback: expect.objectContaining({
        message: 'Good structure.',
        weakArea: 'Detail',
      }),
    });
  });

  it('attaches a German speaking evaluation immediately after submission', () => {
    const lesson = toLessonDetail(
      {
        id: 20,
        plan_id: 10,
        title: 'Vorstellung und Motivation',
        topic: 'Stellen Sie sich professionell vor.',
        order_number: 1,
        exercises: [
          {
            id: 101,
            lesson_id: 20,
            type: 'speaking_prompt',
            question: 'Stellen Sie sich kurz vor.',
            expected_answer: 'Eine klare professionelle Vorstellung.',
            difficulty: 'A2',
          },
        ],
      },
      { planId: 10, language: 'German', accent: 'blue' },
    );

    const updatedLesson = attachSubmissionToLesson(lesson, {
      answer: {
        id: 500,
        exercise_id: 101,
        answer_text: 'Ich bin Softwareentwicklerin.',
        score: 84,
      },
      transcription: 'Ich bin Softwareentwicklerin.',
      exercise_status: 'finished',
      lesson_progress: 100,
      feedback: {
        message: 'Gute und klare Antwort.',
        weak_area: 'Detail',
        corrected_answer: 'Ich bin Softwareentwicklerin mit Erfahrung in Java.',
      },
    });

    expect(updatedLesson.exercises[0]).toMatchObject({
      status: 'finished',
      score: 84,
      answerText: 'Ich bin Softwareentwicklerin.',
      feedback: expect.objectContaining({
        message: 'Gute und klare Antwort.',
        transcription: 'Ich bin Softwareentwicklerin.',
        improvedExample: 'Ich bin Softwareentwicklerin mit Erfahrung in Java.',
      }),
    });
  });

  it('derives plan and progress summaries used by the dashboard', () => {
    const plans = derivePlanProgress([
      {
        id: 10,
        title: 'Interview German',
        lessons: [
          { id: 20, title: 'Intro', exercises: [{ id: 1, status: 'finished' }, { id: 2, status: 'not-started' }] },
          { id: 21, title: 'Follow-up', exercises: [{ id: 3, score: 90 }] },
        ],
      },
    ]);

    const progress = toProgressSummary(
      {
        user_id: 2,
        completed_exercises: 2,
        total_exercises: 3,
        average_score: 86,
        recent_progress: [{ plan_id: 10, lesson_id: 20, lesson_title: 'Intro', status: 'ongoing', progress: 50 }],
        recent_finished: [{ plan_id: 10, lesson_id: 21, lesson_title: 'Follow-up', score: 90 }],
      },
      plans,
    );

    expect(plans[0]).toMatchObject({ progress: 75, status: 'ongoing' });
    expect(progress).toMatchObject({
      userId: 2,
      completedExercises: 2,
      totalExercises: 3,
      averageScore: 86,
    });
    expect(progress.recentProgress[0]).toMatchObject({ planIndex: 0, lessonIndex: 0, planTitle: 'Interview German' });
    expect(progress.recentFinished[0]).toMatchObject({ status: 'finished', progress: 90, lessonIndex: 1 });
  });
});
