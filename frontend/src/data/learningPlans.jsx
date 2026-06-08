import {
  BriefcaseBusiness,
  GraduationCap,
  Languages,
  MapPinned,
  MessageSquareText,
  Plane,
  Presentation,
  Sparkles,
} from 'lucide-react';

const statusProgress = {
  'not-started': 0,
  ongoing: 45,
  finished: 100,
};

function makeFeedback(type, topic, grade) {
  return {
    title: 'AI feedback',
    score: grade,
    message: `Good work on the ${type} exercise for ${topic}. Your answer is clear and mostly well structured.`,
    strengths: ['Clear main idea', 'Relevant vocabulary', 'Good task focus'],
    improvements: ['Add one more specific detail', 'Use a more natural transition', 'Check formality in the final sentence'],
    improvedExample:
      type === 'speaking'
        ? 'I am currently studying computer science with a focus on backend development, and I enjoy building practical systems that solve real user problems.'
        : 'I am currently studying computer science, specializing in backend development and practical software systems.',
  };
}

function exerciseFeedback(status, type, topic, grade) {
  return status === 'finished' ? makeFeedback(type, topic, grade) : null;
}

function makeExercises(topic, reading, listening, writing, speaking, statuses = {}) {
  const readingStatus = statuses.reading ?? 'not-started';
  const listeningStatus = statuses.listening ?? 'not-started';
  const writingStatus = statuses.writing ?? 'not-started';
  const speakingStatus = statuses.speaking ?? 'not-started';
  const readingGrade = readingStatus === 'finished' ? 88 : null;
  const listeningGrade = listeningStatus === 'finished' ? 84 : null;
  const writingGrade = writingStatus === 'finished' ? 91 : null;
  const speakingGrade = speakingStatus === 'finished' ? 86 : null;

  return [
    {
      title: 'Reading practice',
      type: 'reading',
      status: readingStatus,
      grade: readingGrade,
      feedback: exerciseFeedback(readingStatus, 'reading', topic, readingGrade),
      task: reading,
      prompt: `Complete a quick language accuracy task for: ${topic}`,
      format: 'Fill in the blank and multiple choice',
    },
    {
      title: 'Listening practice',
      type: 'listening',
      status: listeningStatus,
      grade: listeningGrade,
      feedback: exerciseFeedback(listeningStatus, 'listening', topic, listeningGrade),
      task: listening,
      prompt: `Listen for the key idea and choose the most professional response for: ${topic}`,
      format: 'Audio scenario with answer choices',
    },
    {
      title: 'Writing practice',
      type: 'writing',
      status: writingStatus,
      grade: writingGrade,
      feedback: exerciseFeedback(writingStatus, 'writing', topic, writingGrade),
      task: writing,
      prompt: `Write a short answer using clear structure for: ${topic}`,
      format: 'Short written answer',
    },
    {
      title: 'Speaking practice',
      type: 'speaking',
      status: speakingStatus,
      grade: speakingGrade,
      feedback: exerciseFeedback(speakingStatus, 'speaking', topic, speakingGrade),
      task: speaking,
      prompt: `Record a concise spoken answer for: ${topic}`,
      format: 'Voice response',
    },
  ];
}

function lessonStatus(exercises) {
  if (exercises.every((exercise) => exercise.status === 'finished')) {
    return 'finished';
  }

  if (exercises.some((exercise) => exercise.status === 'ongoing' || exercise.status === 'finished')) {
    return 'ongoing';
  }

  return 'not-started';
}

function lessonProgress(exercises) {
  const total = exercises.reduce((sum, exercise) => sum + statusProgress[exercise.status], 0);
  return Math.round(total / exercises.length);
}

function createDefaultBlocks(config) {
  return [
    {
      type: 'content',
      title: 'Core idea',
      subtitle: config.time,
      text: config.text,
      points: config.learningPoints.slice(0, 2),
      visual: {
        label: 'Flow',
        steps: ['Prepare', 'Structure', 'Practice'],
      },
    },
    {
      type: 'exercise',
      exerciseIndex: 0,
    },
    {
      type: 'content',
      title: 'Useful language pattern',
      subtitle: 'Apply it immediately',
      text: `Use the lesson topic as your guide: ${config.topic}`,
      points: config.learningPoints.slice(2),
      visual: {
        label: 'Pattern',
        steps: ['Opening', 'Detail', 'Result'],
      },
    },
    {
      type: 'exercise',
      exerciseIndex: 1,
    },
    {
      type: 'content',
      title: 'Production practice',
      subtitle: 'Move from recognition to output',
      text: 'After recognizing useful phrases, practice producing your own answer in writing and speaking.',
      points: [
        'Keep your answer short enough to remember.',
        'Use one concrete detail instead of several vague claims.',
      ],
      visual: {
        label: 'Output',
        steps: ['Draft', 'Speak', 'Refine'],
      },
    },
    {
      type: 'exercise',
      exerciseIndex: 2,
    },
    {
      type: 'exercise',
      exerciseIndex: 3,
    },
  ];
}

function buildLesson(config) {
  const status = lessonStatus(config.exercises);

  return {
    ...config,
    blocks: config.blocks ?? createDefaultBlocks(config),
    status,
    progress: lessonProgress(config.exercises),
    comment:
      status === 'finished'
        ? `Great work. You finished every exercise in ${config.title} and are ready to reuse these patterns in a real situation.`
        : null,
  };
}

const interviewLessons = [
  buildLesson({
    title: 'Self Introduction',
    topic: 'Introduce yourself professionally in an interview.',
    icon: MessageSquareText,
    accent: 'blue',
    time: '8 min',
    components: ['Text guide', 'Answer structure', 'Practice exercises', 'Summary'],
    learningPoints: [
      'Use a three-part structure: identity, relevant background, and professional direction.',
      'Keep the first answer under one minute and avoid unrelated personal details.',
      'Replace casual words with interview-ready vocabulary such as contributed, developed, supported, and collaborated.',
    ],
    text: 'A strong self-introduction is short, professional, and relevant. Start with who you are, mention your field or current role, then connect your experience to the position.',
    exercises: makeExercises(
      'professional self-introduction',
      'Choose the best phrase to complete: “I am currently ___ computer science with a focus on backend development.”',
      'A candidate introduces themselves too casually. Select the improved opening.',
      'Write a 4 sentence introduction for a software internship interview.',
      'Record a 45 second answer to “Tell me about yourself.”',
      { reading: 'finished', listening: 'finished', writing: 'finished', speaking: 'finished' },
    ),
    summary:
      'By the end of this lesson, you should be able to introduce yourself with a clear role, relevant background, and confident professional tone.',
  }),
  buildLesson({
    title: 'Education and Background',
    topic: 'Explain your studies, university, and academic background.',
    icon: GraduationCap,
    accent: 'green',
    time: '7 min',
    components: ['Text guide', 'Study vocabulary', 'Practice exercises', 'Summary'],
    learningPoints: [
      'State your degree, university, specialization, and graduation status in one clear answer.',
      'Connect your academic choices to skills the interviewer cares about.',
      'Use precise phrases such as I specialize in, my coursework focuses on, and I expect to graduate in.',
    ],
    text: 'When explaining your education, name your degree, university, specialization, and academic focus. Keep the answer clear and connect your studies to your career direction.',
    exercises: makeExercises(
      'education and academic background',
      'Pick the most natural sentence for describing a master’s specialization.',
      'Listen to two graduation-status answers and choose the clearer one.',
      'Write a short answer explaining why you chose your field.',
      'Say your degree, university, specialization, and graduation status clearly.',
      { reading: 'finished', listening: 'ongoing' },
    ),
    summary:
      'This lesson helps you explain your academic profile without sounding too long or too vague.',
  }),
  buildLesson({
    title: 'Work Experience and Internships',
    topic: 'Talk about previous internships, jobs, or projects.',
    icon: BriefcaseBusiness,
    accent: 'amber',
    time: '9 min',
    components: ['Text guide', 'Responsibility verbs', 'Practice exercises', 'Summary'],
    learningPoints: [
      'Use context, responsibility, result, and learning as your answer structure.',
      'Mention concrete actions instead of saying “I helped with many things.”',
      'Use active verbs such as implemented, tested, analyzed, maintained, and coordinated.',
    ],
    text: 'Work experience answers should explain the context, your responsibilities, and the result. Use active verbs and mention one concrete thing you learned.',
    exercises: makeExercises(
      'work experience and internships',
      'Fill in the blank with the strongest action verb for an internship responsibility.',
      'Listen to an internship description and identify the responsibility mentioned.',
      'Write a short description of one internship or project responsibility.',
      'Explain one work experience and one lesson learned from it.',
    ),
    summary:
      'This lesson prepares you to describe experience with evidence, action, and reflection.',
  }),
  buildLesson({
    title: 'Project Explanation',
    topic: 'Present a technical or academic project clearly.',
    icon: Presentation,
    accent: 'violet',
    time: '10 min',
    components: ['Structure guide', 'Technical simplification', 'Practice exercises', 'Summary'],
    learningPoints: [
      'Explain the problem before explaining the technology.',
      'Separate your personal role from the team result.',
      'Adapt technical depth based on the interviewer’s background.',
    ],
    text: 'A good project explanation follows a simple structure: problem, solution, your role, and impact. For non-technical listeners, avoid jargon and explain the value first.',
    exercises: makeExercises(
      'technical or academic project explanation',
      'Choose the clearest non-technical explanation of an API-based project.',
      'Listen to a project answer and identify the missing part: problem, solution, role, or impact.',
      'Write a problem-solution-role-impact explanation for one project.',
      'Explain your project to a non-technical interviewer in 60 seconds.',
    ),
    summary:
      'This lesson helps you turn a technical project into a clear interview story.',
  }),
  buildLesson({
    title: 'Strengths and Weaknesses',
    topic: 'Answer common HR questions about strengths and weaknesses.',
    icon: Sparkles,
    accent: 'rose',
    time: '8 min',
    components: ['Answer framework', 'Example bank', 'Practice exercises', 'Summary'],
    learningPoints: [
      'Support each strength with a short example.',
      'Choose a weakness that is honest but not central to the job.',
      'Always explain how you are actively improving the weakness.',
    ],
    text: 'Strengths need short examples. Weaknesses should sound honest but professional: name the weakness, explain what you are doing to improve, and avoid critical job requirements.',
    exercises: makeExercises(
      'strengths and weaknesses',
      'Choose the strongest weakness answer from three options.',
      'Listen to a strength answer and identify the example.',
      'Rewrite a vague weakness answer into a professional one.',
      'Answer: “What are your strengths and weaknesses?” in a balanced way.',
    ),
    summary:
      'This lesson helps you answer HR questions with confidence, honesty, and professional framing.',
  }),
];

const italyLessons = [
  buildLesson({
    title: 'Italian Travel Basics',
    topic: 'Greet people, introduce yourself, and handle polite travel phrases.',
    icon: Languages,
    accent: 'green',
    time: '12 min',
    components: ['Phrase list', 'Dialogue pattern', 'Practice exercises', 'Summary'],
    learningPoints: [
      'Use polite greetings before asking for help.',
      'Memorize flexible sentence starters you can reuse in many travel situations.',
      'Practice saying your name, country, and travel purpose clearly.',
    ],
    text: 'Start with high-frequency travel phrases. Practice greetings, your name, where you are from, and polite expressions you can reuse in hotels, cafes, and shops.',
    exercises: makeExercises(
      'Italian travel basics',
      'Choose the correct greeting for entering a small shop in the afternoon.',
      'Listen to a hotel greeting and pick the right polite reply.',
      'Write a 3 sentence introduction as a traveler in Italy.',
      'Say your name, country, and how long you will stay.',
      { reading: 'finished', listening: 'ongoing' },
    ),
    summary:
      'This lesson gives you reusable basic phrases for first contact in hotels, cafes, and shops.',
  }),
  buildLesson({
    title: 'Ordering Food',
    topic: 'Order meals, ask about ingredients, and pay at restaurants.',
    icon: MessageSquareText,
    accent: 'amber',
    time: '15 min',
    components: ['Menu vocabulary', 'Ordering pattern', 'Practice exercises', 'Summary'],
    learningPoints: [
      'Use vorrei or posso avere to order politely.',
      'Ask simple ingredient questions before choosing food.',
      'Close the interaction by asking for the bill politely.',
    ],
    text: 'Ordering food is about polite requests and useful menu words. Practice asking for items, checking ingredients, and closing the interaction naturally.',
    exercises: makeExercises(
      'ordering food in Italy',
      'Select the correct phrase to order a coffee and pastry.',
      'Listen to a waiter describe a dish and identify whether it contains meat.',
      'Write a short restaurant order with one ingredient question.',
      'Role-play ordering lunch and asking for the bill.',
      { reading: 'ongoing' },
    ),
    summary:
      'This lesson prepares you for simple restaurant and cafe interactions.',
  }),
  buildLesson({
    title: 'Asking for Directions',
    topic: 'Find stations, hotels, landmarks, and basic transport routes.',
    icon: MapPinned,
    accent: 'blue',
    time: '14 min',
    components: ['Map phrases', 'Direction words', 'Practice exercises', 'Summary'],
    learningPoints: [
      'Recognize left, right, straight ahead, nearby, and far away.',
      'Ask for help using short questions instead of complex sentences.',
      'Confirm the destination to avoid misunderstanding.',
    ],
    text: 'Direction practice focuses on a small set of phrases: where something is, how to get there, and whether it is nearby. Learn the response words before long sentences.',
    exercises: makeExercises(
      'asking for directions in Italy',
      'Match direction words to icons: left, right, straight ahead, nearby.',
      'Listen to a short direction and choose the correct route.',
      'Write a question asking how to get to the train station.',
      'Say that you are lost and need help finding your hotel.',
    ),
    summary:
      'This lesson helps you ask for and understand basic directions while traveling.',
  }),
  buildLesson({
    title: 'Daily Travel Problems',
    topic: 'Handle simple problems at hotels, shops, and ticket counters.',
    icon: Plane,
    accent: 'violet',
    time: '13 min',
    components: ['Useful sentences', 'Problem pattern', 'Practice exercises', 'Summary'],
    learningPoints: [
      'State the problem first, then say what you need.',
      'Use short sentences when stressed or in a hurry.',
      'Ask one clarifying question to confirm the next step.',
    ],
    text: 'Travel problems need calm, simple sentences. Practice saying what happened, what you need, and one clarifying question.',
    exercises: makeExercises(
      'daily travel problems in Italy',
      'Choose the best sentence for reporting a missing item.',
      'Listen to a ticket-counter announcement and identify the delay.',
      'Write a short hotel problem message.',
      'Explain that your reservation is under your name and ask for help.',
    ),
    summary:
      'This lesson prepares you to stay calm and communicate basic travel problems clearly.',
  }),
];

export const learningPlans = [
  {
    title: 'German Job Interview Preparation',
    description: 'Personalized plan to help you succeed in German job interviews.',
    summary:
      'Build confident answers for common HR and technical interview questions with short text guides, speaking practice, and answer improvement tasks.',
    language: 'German',
    goal: 'Professional interview',
    duration: '4 weeks',
    accent: 'blue',
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
    accent: 'green',
    progress: 18,
    lessons: italyLessons,
  },
];

export const weeklyPlan = [
  { day: 'M', done: true },
  { day: 'T', done: true },
  { day: 'W', done: false },
  { day: 'T', done: true },
  { day: 'F', done: false },
  { day: 'S', done: false },
  { day: 'S', done: false },
];
