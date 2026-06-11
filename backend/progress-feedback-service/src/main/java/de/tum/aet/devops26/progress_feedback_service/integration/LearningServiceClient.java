package de.tum.aet.devops26.progress_feedback_service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LearningServiceClient {

    private final RestClient restClient;

    public LearningServiceClient(
        RestClient.Builder restClientBuilder,
        @Value("${services.learning.base-url}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public ExerciseContext getExercise(Long lessonId, Long exerciseId) {
        try {
            LessonContext lesson = restClient.get()
                .uri("/api/v1/lessons/{lessonId}", lessonId)
                .retrieve()
                .body(LessonContext.class);

            if (lesson == null || lesson.exercises() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Learning service returned no exercises.");
            }

            return lesson.exercises().stream()
                .filter(exercise -> exerciseId.equals(exercise.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found."));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to load exercise context.", exception);
        }
    }

    private record LessonContext(List<ExerciseContext> exercises) {
    }

    public record ExerciseContext(
        Long id,
        String type,
        String subtype,
        String question,
        @JsonProperty("expected_answer") String expectedAnswer,
        String difficulty
    ) {
    }
}
