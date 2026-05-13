export function getRecentLessons(learningPlans) {
  return learningPlans
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
}
