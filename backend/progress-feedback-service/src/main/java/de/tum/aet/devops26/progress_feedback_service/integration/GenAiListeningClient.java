package de.tum.aet.devops26.progress_feedback_service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.progress_feedback_service.security.CurrentBearerTokenResolver;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GenAiListeningClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenAiListeningClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final CurrentBearerTokenResolver currentBearerTokenResolver;
    private final boolean authEnabled;

    public GenAiListeningClient(
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

    public ListeningGenerateResponse generate(ListeningGenerateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listening generate request must not be null.");
        }

        Optional<String> bearerToken = currentBearerTokenResolver.resolveTokenValue();
        if (authEnabled && bearerToken.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated listening generation requires a Bearer token.");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("target_language", request.targetLanguage());
            payload.put("level", request.level());
            if (request.topic() != null && !request.topic().isBlank()) {
                payload.put("topic", request.topic());
            }

            LOGGER.info("Requesting listening exercise generation for language={} level={}", request.targetLanguage(), request.level());

            String jsonBody = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/genai/listening/generate"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            bearerToken.ifPresent(token -> requestBuilder.header("Authorization", "Bearer " + token));

            HttpResponse<String> httpResponse = httpClient
                    .sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .orTimeout(180, TimeUnit.SECONDS)
                    .join();

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                LOGGER.warn("GenAI listening generation rejected: {}", httpResponse.body());
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "GenAI service rejected the listening generation: " + httpResponse.body());
            }

            return objectMapper.readValue(httpResponse.body(), ListeningGenerateResponse.class);

        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Could not serialize or parse GenAI listening request.",
                    exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to generate listening exercise.",
                    exception);
        }
    }

    public record ListeningGenerateRequest(
            String targetLanguage,
            String level,
            String topic) {
    }

    public record ListeningGenerateResponse(
            String script,
            List<ListeningQuestion> questions,
            @JsonProperty("script_audio_b64") String scriptAudioB64) {
    }

    public record ListeningQuestion(
            String question,
            List<ListeningOption> options,
            String explanation) {
    }

    public record ListeningOption(
            String text,
            @JsonProperty("is_correct") boolean isCorrect) {
    }
}
