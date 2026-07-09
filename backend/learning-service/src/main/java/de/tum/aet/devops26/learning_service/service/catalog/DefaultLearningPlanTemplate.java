package de.tum.aet.devops26.learning_service.service.catalog;

import java.util.Map;

public record DefaultLearningPlanTemplate(
    String key,
    Map<String, DefaultLearningPlanContent> languages
) {
}
