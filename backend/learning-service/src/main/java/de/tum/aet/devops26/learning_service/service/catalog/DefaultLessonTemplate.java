package de.tum.aet.devops26.learning_service.service.catalog;

import java.util.List;

public record DefaultLessonTemplate(
    String title,
    String topic,
    List<String> exercises
) {
}
