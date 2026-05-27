function flattenLessons(learningPlans) {
  return learningPlans
    .flatMap((plan, planIndex) =>
      plan.lessons.map((lesson, lessonIndex) => ({
        ...lesson,
        planIndex,
        lessonIndex,
        planTitle: plan.title,
      })),
    );
}

export function getRecentLessons(learningPlans) {
  return flattenLessons(learningPlans)
    .filter((lesson) => lesson.status === 'ongoing')
    .sort((a, b) => b.progress - a.progress)
    .slice(0, 3);
}

export function getRecentFinishedLessons(learningPlans) {
  return flattenLessons(learningPlans)
    .filter((lesson) => lesson.status === 'finished')
    .sort((a, b) => b.progress - a.progress)
    .slice(0, 3);
}
