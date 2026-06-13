package de.tum.aet.devops26.progress_feedback_service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.tum.aet.devops26.progress_feedback_service.security.CurrentBearerTokenResolver;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LearningServiceClient {

    private final RestClient restClient;
    private final CurrentBearerTokenResolver currentBearerTokenResolver;
    private final boolean authEnabled;

    public LearningServiceClient(
        RestClient.Builder restClientBuilder,
        @Value("${services.learning.base-url}") String baseUrl,
        CurrentBearerTokenResolver currentBearerTokenResolver,
        @Value("${app.auth.enabled:false}") boolean authEnabled
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.currentBearerTokenResolver = currentBearerTokenResolver;
        this.authEnabled = authEnabled;
    }

    public ExerciseContext getExercise(Long lessonId, Long exerciseId) {
        Optional<String> bearerToken = currentBearerTokenResolver.resolveTokenValue();
        if (authEnabled && bearerToken.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated learning context lookup requires a Bearer token."
            );
        }

        try {
            RequestHeadersSpec<?> request = restClient.get().uri("/api/v1/lessons/{lessonId}", lessonId);
            bearerToken.ifPresent(token -> request.header("Authorization", "Bearer " + token));

            LessonContext lesson = request.retrieve().body(LessonContext.class);

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
