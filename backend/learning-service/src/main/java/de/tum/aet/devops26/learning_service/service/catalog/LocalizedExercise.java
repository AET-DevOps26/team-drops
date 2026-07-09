package de.tum.aet.devops26.learning_service.service.catalog;

import java.util.List;

public record LocalizedExercise(
    String question,
    String expectedAnswer,
    String format,
    List<String> keywords
) {
}
