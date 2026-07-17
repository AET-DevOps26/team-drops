DELETE FROM learning_plans plan
WHERE plan.origin = 'AI_RAG'
  AND NOT EXISTS (
      SELECT 1
      FROM lessons lesson
      WHERE lesson.plan_id = plan.id
  );
