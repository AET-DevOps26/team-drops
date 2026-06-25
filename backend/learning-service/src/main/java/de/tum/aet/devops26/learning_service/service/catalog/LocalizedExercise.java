package de.tum.aet.devops26.learning_service.service.catalog;

public record LocalizedExercise(
    String question,
    String expectedAnswer,
    String format
) {
}
