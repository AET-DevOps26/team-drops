package de.tum.aet.devops26.learning_service.service.catalog;

import java.util.List;

public record DefaultLearningPlanTemplate(
    String key,
    String title,
    String description,
    String duration,
    String defaultGoal,
    String defaultLanguage,
    String defaultLevel,
    String defaultExpectedAnswer,
    List<DefaultLessonTemplate> lessons
) {
}
