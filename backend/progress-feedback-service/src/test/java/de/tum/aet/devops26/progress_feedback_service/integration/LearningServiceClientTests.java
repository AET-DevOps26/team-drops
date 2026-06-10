package de.tum.aet.devops26.progress_feedback_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient.ExerciseContext;
import de.tum.aet.devops26.progress_feedback_service.security.CurrentBearerTokenResolver;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class LearningServiceClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getExerciseDoesNotSendAuthorizationHeaderWhenAuthIsDisabled() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        LearningServiceClient client = newClient(false, Optional.empty(), authorizationHeader);

        ExerciseContext exercise = client.getExercise(3L, 7L);

        assertThat(exercise.id()).isEqualTo(7L);
        assertThat(authorizationHeader.get()).isNull();
    }

    @Test
    void getExerciseSendsAuthorizationHeaderWhenTokenExists() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        LearningServiceClient client = newClient(true, Optional.of("test-token"), authorizationHeader);

        ExerciseContext exercise = client.getExercise(3L, 7L);

        assertThat(exercise.id()).isEqualTo(7L);
        assertThat(authorizationHeader.get()).isEqualTo("Bearer test-token");
    }

    @Test
    void getExerciseFailsClearlyWhenAuthIsEnabledAndTokenIsMissing() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        LearningServiceClient client = newClient(true, Optional.empty(), authorizationHeader);

        assertThatThrownBy(() -> client.getExercise(3L, 7L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Authenticated learning context lookup requires a Bearer token");

        assertThat(authorizationHeader.get()).isNull();
    }

    private LearningServiceClient newClient(
        boolean authEnabled,
        Optional<String> bearerToken,
        AtomicReference<String> authorizationHeader
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/lessons/3", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sendJson(exchange);
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new LearningServiceClient(
            RestClient.builder(),
            baseUrl,
            new StubBearerTokenResolver(bearerToken),
            authEnabled
        );
    }

    private void sendJson(HttpExchange exchange) throws IOException {
        byte[] body = """
            {
              "exercises": [
                {
                  "id": 7,
                  "type": "writing",
                  "subtype": "translation",
                  "question": "Translate: I would like a coffee",
                  "expected_answer": "Je voudrais un cafe",
                  "difficulty": "A2"
                }
              ]
            }
            """.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
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
