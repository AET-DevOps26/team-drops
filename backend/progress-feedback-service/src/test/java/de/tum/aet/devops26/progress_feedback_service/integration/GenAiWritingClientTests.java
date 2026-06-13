package de.tum.aet.devops26.progress_feedback_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationRequest;
import de.tum.aet.devops26.progress_feedback_service.security.CurrentBearerTokenResolver;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GenAiWritingClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void evaluateDoesNotSendAuthorizationHeaderWhenAuthIsDisabled() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiWritingClient client = newClient(false, Optional.empty(), authorizationHeader);

        client.evaluate(request());

        assertThat(authorizationHeader.get()).isNull();
    }

    @Test
    void evaluateSendsAuthorizationHeaderWhenTokenExists() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiWritingClient client = newClient(true, Optional.of("test-token"), authorizationHeader);

        client.evaluate(request());

        assertThat(authorizationHeader.get()).isEqualTo("Bearer test-token");
    }

    @Test
    void evaluateFailsClearlyWhenAuthIsEnabledAndTokenIsMissing() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiWritingClient client = newClient(true, Optional.empty(), authorizationHeader);

        assertThatThrownBy(() -> client.evaluate(request()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Authenticated writing evaluation requires a Bearer token");

        assertThat(authorizationHeader.get()).isNull();
    }

    private GenAiWritingClient newClient(
        boolean authEnabled,
        Optional<String> bearerToken,
        AtomicReference<String> authorizationHeader
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/genai/writing/evaluate", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sendJson(exchange);
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new GenAiWritingClient(
            new ObjectMapper(),
            baseUrl,
            new StubBearerTokenResolver(bearerToken),
            authEnabled
        );
    }

    private void sendJson(HttpExchange exchange) throws IOException {
        byte[] body = """
            {
              "score": 8.5,
              "is_correct": true,
              "message": "Good answer.",
              "weak_area": "grammar",
              "corrected_answer": "Corrected answer."
            }
            """.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    private WritingEvaluationRequest request() {
        return new WritingEvaluationRequest(
            42L,
            7L,
            "writing",
            "Describe your education.",
            "I studied computer science.",
            "Ich habe Informatik studiert.",
            "German",
            "A1"
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
