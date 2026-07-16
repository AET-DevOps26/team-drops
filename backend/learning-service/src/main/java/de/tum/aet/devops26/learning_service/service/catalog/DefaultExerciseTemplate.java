package de.tum.aet.devops26.learning_service.service.catalog;

import java.util.List;

public record DefaultExerciseTemplate(
    String question,
    String subtype,
    String expectedAnswer,
    List<String> keywords
) {
}
