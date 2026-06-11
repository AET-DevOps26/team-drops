package de.tum.aet.devops26.progress_feedback_service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GenAiWritingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenAiWritingClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public GenAiWritingClient(
            ObjectMapper objectMapper,
            @Value("${services.genai.base-url}") String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public WritingEvaluationResponse evaluate(WritingEvaluationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Writing evaluation request must not be null.");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("user_id", request.userId());
            payload.put("exercise_id", request.exerciseId());
            payload.put("exercise_type", request.exerciseType());
            payload.put("question", request.question());
            payload.put("expected_answer", request.expectedAnswer());
            payload.put("user_answer", request.userAnswer());
            payload.put("target_language", request.targetLanguage());
            payload.put("level", request.level());

            LOGGER.info("Sending GenAI writing request body for exercise {}", request.exerciseId());

            String jsonBody = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/genai/writing/evaluate"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                LOGGER.warn("GenAI writing evaluation rejected: {}", httpResponse.body());
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "GenAI service rejected the evaluation: " + httpResponse.body());
            }

            WritingEvaluationResponse response = objectMapper.readValue(
                    httpResponse.body(), WritingEvaluationResponse.class);
            return response;

        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Could not serialize or parse GenAI writing request.",
                    exception);

        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to evaluate written answer.",
                    exception);
        }
    }

    public record WritingEvaluationRequest(
            @JsonProperty("user_id") Long userId,
            @JsonProperty("exercise_id") Long exerciseId,
            @JsonProperty("exercise_type") String exerciseType,
            String question,
            @JsonProperty("expected_answer") String expectedAnswer,
            @JsonProperty("user_answer") String userAnswer,
            @JsonProperty("target_language") String targetLanguage,
            String level) {
    }

    public record WritingEvaluationResponse(
            double score,
            @JsonProperty("is_correct") boolean isCorrect,
            String message,
            @JsonProperty("weak_area") String weakArea,
            @JsonProperty("corrected_answer") String correctedAnswer) {
    }
}