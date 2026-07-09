package de.tum.aet.devops26.progress_feedback_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiSpeakingClient.SpeakingEvaluationRequest;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiSpeakingClient.SpeakingEvaluationResponse;
import de.tum.aet.devops26.progress_feedback_service.security.CurrentBearerTokenResolver;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GenAiSpeakingClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void evaluateSendsMultipartBodyAndMapsResponse() throws IOException {
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        GenAiSpeakingClient client = newClient(
            200,
            successJson(),
            true,
            Optional.of("test-token"),
            contentType,
            authorizationHeader,
            requestBody
        );

        SpeakingEvaluationResponse response = client.evaluate(request());

        assertThat(contentType.get()).startsWith("multipart/form-data; boundary=");
        assertThat(authorizationHeader.get()).isEqualTo("Bearer test-token");
        assertThat(requestBody.get()).contains("name=\"audio\"; filename=\"answer.webm\"");
        assertThat(requestBody.get()).contains("Content-Type: audio/webm");
        assertThat(requestBody.get()).contains("name=\"exercise_type\"");
        assertThat(requestBody.get()).contains("translation");
        assertThat(requestBody.get()).contains("name=\"question\"");
        assertThat(requestBody.get()).contains("Translate: The cat is on the table");
        assertThat(requestBody.get()).contains("name=\"expected_answer\"");
        assertThat(requestBody.get()).contains("Die Katze ist auf dem Tisch");
        assertThat(requestBody.get()).contains("name=\"target_language\"");
        assertThat(requestBody.get()).contains("German");
        assertThat(requestBody.get()).contains("name=\"level\"");
        assertThat(requestBody.get()).contains("A2");
        assertThat(response.transcription()).isEqualTo("Die Katze ist an den Tisch");
        assertThat(response.score()).isEqualTo(6.5);
        assertThat(response.isCorrect()).isFalse();
        assertThat(response.message()).isEqualTo("Check the preposition.");
        assertThat(response.weakArea()).isEqualTo("grammar");
        assertThat(response.correctedAnswer()).isEqualTo("Die Katze ist auf dem Tisch");
        assertThat(response.feedbackAudioB64()).isEqualTo("UklGRg==");
    }

    @Test
    void evaluateRequiresBearerTokenWhenAuthEnabled() {
        GenAiSpeakingClient client = new GenAiSpeakingClient(
            new ObjectMapper(),
            "http://localhost:1",
            new StubBearerTokenResolver(Optional.empty()),
            true
        );

        assertThatThrownBy(() -> client.evaluate(request()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void evaluateRejectsNullRequest() {
        GenAiSpeakingClient client = new GenAiSpeakingClient(
            new ObjectMapper(),
            "http://localhost:1",
            new StubBearerTokenResolver(Optional.empty()),
            false
        );

        assertThatThrownBy(() -> client.evaluate(null))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void evaluateMapsGenAiFailureToBadGateway() throws IOException {
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        GenAiSpeakingClient client = newClient(
            502,
            "{\"detail\":\"down\"}",
            false,
            Optional.empty(),
            contentType,
            authorizationHeader,
            requestBody
        );

        assertThatThrownBy(() -> client.evaluate(request()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_GATEWAY);

        assertThat(authorizationHeader.get()).isNull();
    }

    @Test
    void evaluateMapsPayloadTooLargeToPayloadTooLarge() throws IOException {
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        GenAiSpeakingClient client = newClient(
            413,
            "{\"detail\":\"Audio file exceeds 25 MB limit\"}",
            false,
            Optional.empty(),
            contentType,
            authorizationHeader,
            requestBody
        );

        assertThatThrownBy(() -> client.evaluate(request()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void evaluateAllowsResponseWithoutFeedbackAudio() throws IOException {
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        GenAiSpeakingClient client = newClient(
            200,
            """
                {
                  "transcription": "Die Katze ist auf dem Tisch",
                  "score": 8.0,
                  "is_correct": true,
                  "message": "Clear and accurate.",
                  "weak_area": "pronunciation",
                  "corrected_answer": "Die Katze ist auf dem Tisch"
                }
                """,
            false,
            Optional.empty(),
            contentType,
            authorizationHeader,
            requestBody
        );

        SpeakingEvaluationResponse response = client.evaluate(request());

        assertThat(response.transcription()).isEqualTo("Die Katze ist auf dem Tisch");
        assertThat(response.score()).isEqualTo(8.0);
        assertThat(response.isCorrect()).isTrue();
        assertThat(response.feedbackAudioB64()).isNull();
    }

    private GenAiSpeakingClient newClient(
        int statusCode,
        String responseJson,
        boolean authEnabled,
        Optional<String> bearerToken,
        AtomicReference<String> contentType,
        AtomicReference<String> authorizationHeader,
        AtomicReference<String> requestBody
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/genai/speaking/evaluate", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, statusCode, responseJson);
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new GenAiSpeakingClient(
            new ObjectMapper(),
            baseUrl,
            new StubBearerTokenResolver(bearerToken),
            authEnabled
        );
    }

    private void sendJson(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] body = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    private String successJson() {
        return """
            {
              "transcription": "Die Katze ist an den Tisch",
              "score": 6.5,
              "is_correct": false,
              "message": "Check the preposition.",
              "weak_area": "grammar",
              "corrected_answer": "Die Katze ist auf dem Tisch",
              "feedback_audio_b64": "UklGRg=="
            }
            """;
    }

    private SpeakingEvaluationRequest request() {
        return new SpeakingEvaluationRequest(
            42L,
            7L,
            "audio-bytes".getBytes(StandardCharsets.UTF_8),
            "answer.webm",
            "audio/webm",
            "translation",
            "Translate: The cat is on the table",
            "Die Katze ist auf dem Tisch",
            "German",
            "A2"
        );
    }

    private static class StubBearerTokenResolver extends CurrentBearerTokenResolver {

        private final Optional<String> bearerToken;

        private StubBearerTokenResolver(Optional<String> bearerToken) {
            this.bearerToken = bearerToken;
        }

        @Override
        public Optional<String> resolveTokenValue() {
            return bearerToken;
        }
    }
}
