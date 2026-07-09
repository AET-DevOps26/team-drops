package de.tum.aet.devops26.progress_feedback_service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.progress_feedback_service.security.CurrentBearerTokenResolver;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GenAiSpeakingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenAiSpeakingClient.class);
    private static final String DEFAULT_AUDIO_FILENAME = "answer.webm";
    private static final String DEFAULT_AUDIO_CONTENT_TYPE = "application/octet-stream";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final CurrentBearerTokenResolver currentBearerTokenResolver;
    private final boolean authEnabled;

    public GenAiSpeakingClient(
            ObjectMapper objectMapper,
            @Value("${services.genai.base-url}") String baseUrl,
            CurrentBearerTokenResolver currentBearerTokenResolver,
            @Value("${app.auth.enabled:false}") boolean authEnabled) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.currentBearerTokenResolver = currentBearerTokenResolver;
        this.authEnabled = authEnabled;
    }

    public SpeakingEvaluationResponse evaluate(SpeakingEvaluationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Speaking evaluation request must not be null.");
        }

        Optional<String> bearerToken = currentBearerTokenResolver.resolveTokenValue();
        if (authEnabled && bearerToken.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated speaking evaluation requires a Bearer token.");
        }

        try {
            String boundary = "----team-drops-speaking-" + UUID.randomUUID();
            byte[] multipartBody = buildMultipartBody(request, boundary);

            LOGGER.info("Sending GenAI speaking request body for exercise {}", request.exerciseId());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(
                            URI.create(baseUrl + "/api/v1/genai/speaking/evaluate"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody));

            bearerToken.ifPresent(token -> requestBuilder.header("Authorization", "Bearer " + token));

            HttpResponse<String> httpResponse = httpClient
                    .sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .orTimeout(180, TimeUnit.SECONDS)
                    .join();

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                LOGGER.warn("GenAI speaking evaluation rejected with status {}", httpResponse.statusCode());
                if (httpResponse.statusCode() == HttpStatus.PAYLOAD_TOO_LARGE.value()) {
                    throw new ResponseStatusException(
                            HttpStatus.PAYLOAD_TOO_LARGE,
                            "Uploaded audio exceeds the GenAI speaking limit.");
                }
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "GenAI service rejected the speaking evaluation.");
            }

            return objectMapper.readValue(httpResponse.body(), SpeakingEvaluationResponse.class);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not parse GenAI speaking response.",
                    exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to evaluate spoken answer.",
                    exception);
        }
    }

    private byte[] buildMultipartBody(SpeakingEvaluationRequest request, String boundary) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writeTextPart(output, boundary, "exercise_type", request.exerciseType());
        writeTextPart(output, boundary, "question", request.question());
        writeTextPart(output, boundary, "expected_answer", request.expectedAnswer());
        writeTextPart(output, boundary, "target_language", request.targetLanguage());
        writeTextPart(output, boundary, "level", request.level());
        writeFilePart(output, boundary, request);
        writeAscii(output, "--" + boundary + "--\r\n");

        return output.toByteArray();
    }

    private void writeTextPart(ByteArrayOutputStream output, String boundary, String name, String value) {
        writeAscii(output, "--" + boundary + "\r\n");
        writeAscii(output, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        writeAscii(output, value == null ? "" : value);
        writeAscii(output, "\r\n");
    }

    private void writeFilePart(ByteArrayOutputStream output, String boundary, SpeakingEvaluationRequest request) {
        String filename = valueOrDefault(request.audioFilename(), DEFAULT_AUDIO_FILENAME);
        String contentType = valueOrDefault(request.audioContentType(), DEFAULT_AUDIO_CONTENT_TYPE);

        writeAscii(output, "--" + boundary + "\r\n");
        writeAscii(output, "Content-Disposition: form-data; name=\"audio\"; filename=\"" + sanitizeHeaderValue(filename) + "\"\r\n");
        writeAscii(output, "Content-Type: " + sanitizeHeaderValue(contentType) + "\r\n\r\n");
        output.writeBytes(request.audio() == null ? new byte[0] : request.audio());
        writeAscii(output, "\r\n");
    }

    private void writeAscii(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String sanitizeHeaderValue(String value) {
        return value.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    public record SpeakingEvaluationRequest(
            @JsonProperty("user_id") Long userId,
            @JsonProperty("exercise_id") Long exerciseId,
            byte[] audio,
            String audioFilename,
            String audioContentType,
            @JsonProperty("exercise_type") String exerciseType,
            String question,
            @JsonProperty("expected_answer") String expectedAnswer,
            @JsonProperty("target_language") String targetLanguage,
            String level) {
    }

    public record SpeakingEvaluationResponse(
            String transcription,
            double score,
            @JsonProperty("is_correct") boolean isCorrect,
            String message,
            @JsonProperty("weak_area") String weakArea,
            @JsonProperty("corrected_answer") String correctedAnswer,
            @JsonProperty("feedback_audio_b64") String feedbackAudioB64) {
    }
}
