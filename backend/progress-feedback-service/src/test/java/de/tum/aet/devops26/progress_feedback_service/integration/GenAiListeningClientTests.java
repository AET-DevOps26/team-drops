package de.tum.aet.devops26.progress_feedback_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiListeningClient.ListeningGenerateRequest;
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

class GenAiListeningClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateDoesNotSendAuthorizationHeaderWhenAuthIsDisabled() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiListeningClient client = newClient(false, Optional.empty(), authorizationHeader);

        client.generate(request());

        assertThat(authorizationHeader.get()).isNull();
    }

    @Test
    void generateSendsAuthorizationHeaderWhenTokenExists() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiListeningClient client = newClient(true, Optional.of("test-token"), authorizationHeader);

        client.generate(request());

        assertThat(authorizationHeader.get()).isEqualTo("Bearer test-token");
    }

    @Test
    void generateFailsClearlyWhenAuthIsEnabledAndTokenIsMissing() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiListeningClient client = newClient(true, Optional.empty(), authorizationHeader);

        assertThatThrownBy(() -> client.generate(request()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Authenticated listening generation requires a Bearer token");

        assertThat(authorizationHeader.get()).isNull();
    }

    private GenAiListeningClient newClient(
        boolean authEnabled,
        Optional<String> bearerToken,
        AtomicReference<String> authorizationHeader
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/genai/listening/generate", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sendJson(exchange);
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new GenAiListeningClient(
            new ObjectMapper(),
            baseUrl,
            new StubBearerTokenResolver(bearerToken),
            authEnabled
        );
    }

    private void sendJson(HttpExchange exchange) throws IOException {
        byte[] body = """
            {
              "script": "Guten Morgen.",
              "questions": [
                {
                  "question": "Was sagt die Person?",
                  "options": [
                    {"text": "Guten Morgen", "is_correct": true},
                    {"text": "Gute Nacht", "is_correct": false}
                  ],
                  "explanation": "Die Person sagt Guten Morgen."
                }
              ],
              "script_audio_b64": "UklGRg=="
            }
            """.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    private ListeningGenerateRequest request() {
        return new ListeningGenerateRequest("German", "A2", "daily life");
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
