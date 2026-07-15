import { expect, test } from '@playwright/test';

const user = {
  id: 1,
  name: 'Alex Learner',
  email: 'alex@example.test',
};

const profile = {
  id: 2,
  user_id: 1,
  name: 'Alex Learner',
  country: 'Germany',
  target_language: 'German',
  current_level: 'A2',
  learning_goal: 'Prepare for a software engineering job interview',
};

const plans = [
  {
    id: 201,
    user_id: 1,
    title: 'German Interview Track',
    description: 'Practice interview answers in German.',
    goal: 'Software engineering interview',
    language: 'German',
    level: 'A2',
    duration: '2 weeks',
    status: 'ongoing',
    progress: 0,
    lessons: [
      {
        id: 301,
        plan_id: 201,
        title: 'Self Introduction',
        topic: 'Introduce yourself professionally.',
        order_number: 1,
        status: 'ongoing',
        progress: 0,
        time_estimate_minutes: 10,
        exercise_count: 1,
      },
    ],
  },
];

const lesson = {
  id: 301,
  plan_id: 201,
  title: 'Self Introduction',
  topic: 'Introduce yourself professionally.',
  order_number: 1,
  time_estimate_minutes: 10,
  status: 'ongoing',
  progress: 0,
  exercises: [
    {
      id: 401,
      lesson_id: 301,
      type: 'writing',
      question: 'Write a concise professional introduction.',
      expected_answer: 'A concise introduction with background, motivation, and target role.',
      difficulty: 'A2',
      format: 'Short written answer',
      keywords: ['background', 'motivation'],
    },
  ],
  content_blocks: [
    {
      type: 'content',
      title: 'Interview framing',
      subtitle: '10 min',
      text: 'Use a clear structure before adding details.',
      points: ['Background', 'Motivation', 'Target role'],
    },
    {
      type: 'exercise',
      exercise_id: 401,
    },
  ],
};

async function mockBackend(page) {
  await page.route('**/user-service/api/v1/users/me', (route) => route.fulfill({ json: user }));
  await page.route('**/user-service/api/v1/users/1/profile', (route) => route.fulfill({ json: profile }));
  await page.route('**/learning-service/api/v1/learning-plans/user/1**', (route) => route.fulfill({ json: plans }));
  await page.route('**/learning-service/api/v1/lessons/301**', (route) => route.fulfill({ json: lesson }));
  await page.route('**/progress-service/api/v1/answers/user/1**', (route) => route.fulfill({ json: [] }));
  await page.route('**/progress-service/api/v1/progress/user/1**', (route) => route.fulfill({
    json: {
      user_id: 1,
      completed_exercises: 0,
      total_exercises: 1,
      average_score: 0,
      recent_progress: [],
      recent_finished: [],
      top_lessons: [],
    },
  }));
}

test('learner can bypass auth and open a lesson exercise end to end', async ({ page }) => {
  await mockBackend(page);

  await page.goto('/');
  await page.getByRole('button', { name: 'Continue without authentication' }).click();

  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  await expect(page.getByText('Welcome back, Alex Learner')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Open current learning plan' })).toContainText('German Interview Track');

  await page.getByRole('button', { name: 'Open current learning plan' }).click();
  await expect(page.getByRole('heading', { name: 'Learning Plan' })).toBeVisible();
  await page.getByRole('button', { name: /Self Introduction/ }).click();

  await expect(page.getByRole('heading', { name: 'Lesson' })).toBeVisible();
  await expect(page.getByText('Interview framing')).toBeVisible();

  await page.getByRole('button', { name: /Write a concise professional introduction/ }).click();
  await expect(page.getByRole('heading', { name: 'Exercise' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Task' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Submit answer' })).toBeVisible();
});
