package de.tum.aet.devops26.learning_service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.learning_service.dto.CreateAiLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.ExerciseType;
import de.tum.aet.devops26.learning_service.security.CurrentBearerTokenResolver;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GenAiRagLearningPlanClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenAiRagLearningPlanClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(300);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final CurrentBearerTokenResolver currentBearerTokenResolver;
    private final boolean authEnabled;

    public GenAiRagLearningPlanClient(
        ObjectMapper objectMapper,
        @Value("${services.genai.base-url}") String baseUrl,
        CurrentBearerTokenResolver currentBearerTokenResolver,
        @Value("${app.auth.enabled:false}") boolean authEnabled
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.currentBearerTokenResolver = currentBearerTokenResolver;
        this.authEnabled = authEnabled;
    }

    public RagLearningPlanResponse generate(CreateAiLearningPlanRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "RAG learning-plan request must not be null."
            );
        }
        if (request.getRagTopic() == null || request.getRagTopic().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "RAG learning-plan generation requires a topic."
            );
        }

        Optional<String> bearerToken = currentBearerTokenResolver.resolveTokenValue();
        if (authEnabled && bearerToken.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated RAG learning-plan generation requires a Bearer token."
            );
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("topic", request.getRagTopic());
            payload.put("learning_goal", request.getLearningGoal());
            payload.put("target_language", request.getTargetLanguage());
            payload.put("level", request.getCurrentLevel());
            payload.put("duration_weeks", request.getDurationWeeks());
            payload.put("study_hours_per_week", request.getStudyHoursPerWeek());
            payload.put("minimum_lessons", request.getMinimumLessons());
            payload.put("maximum_lessons", request.getMaximumLessons());
            payload.put("exercise_types", exerciseTypeValues(request.getExerciseTypes()));

            LOGGER.info("Sending GenAI RAG learning-plan request for topic {}", request.getRagTopic());

            String jsonBody = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(
                    URI.create(baseUrl + "/api/v1/genai/rag/learning-plan"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            bearerToken.ifPresent(token -> requestBuilder.header("Authorization", "Bearer " + token));

            HttpResponse<String> httpResponse = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString()
            );

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                LOGGER.warn("GenAI RAG learning-plan generation rejected: {}", httpResponse.body());
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "GenAI service rejected RAG learning-plan generation: " + httpResponse.body()
                );
            }

            return objectMapper.readValue(httpResponse.body(), RagLearningPlanResponse.class);

        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Could not serialize or parse GenAI RAG learning-plan request.",
                exception
            );

        } catch (ResponseStatusException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Unable to generate RAG learning plan.",
                exception
            );
        }
    }

    private List<String> exerciseTypeValues(List<ExerciseType> exerciseTypes) {
        if (exerciseTypes == null) {
            return List.of();
        }
        return exerciseTypes.stream()
            .map(ExerciseType::getValue)
            .toList();
    }

    public record RagLearningPlanResponse(
        String title,
        String description,
        String goal,
        String language,
        String level,
        String duration,
        List<RagLesson> lessons,
        List<RagSource> sources
    ) {
    }

    public record RagLesson(
        String title,
        String topic,
        String summary,
        @JsonProperty("order_number") Integer orderNumber,
        @JsonProperty("content_blocks") List<String> contentBlocks,
        List<RagExercise> exercises
    ) {
    }

    public record RagExercise(
        String type,
        String subtype,
        String question,
        @JsonProperty("expected_answer") String expectedAnswer,
        String difficulty
    ) {
    }

    public record RagSource(
        String source,
        Integer page,
        @JsonProperty("chunk_index") Integer chunkIndex,
        Double score,
        String text
    ) {
    }
}
