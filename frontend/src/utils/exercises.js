export function hasReviewedAnswer(exercise) {
  return Boolean(
    exercise?.feedback
      || exercise?.status === 'finished'
      || exercise?.grade != null
      || exercise?.score != null
      || String(exercise?.answerText ?? '').trim(),
  );
}
