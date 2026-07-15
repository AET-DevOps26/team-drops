package de.tum.aet.devops26.learning_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.tum.aet.devops26.learning_service.security.CurrentBearerTokenResolver;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class UserServiceClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void resolveSubmittedUserIdReturnsSubmittedUserWhenAuthDisabled() {
        UserServiceClient client = new UserServiceClient(
            RestClient.builder(),
            "http://localhost:1",
            new StubBearerTokenResolver(Optional.empty()),
            false
        );

        assertThat(client.resolveSubmittedUserId(42L)).isEqualTo(42L);
    }

    @Test
    void resolveSubmittedUserIdRequiresBearerTokenWhenAuthEnabled() {
        UserServiceClient client = new UserServiceClient(
            RestClient.builder(),
            "http://localhost:1",
            new StubBearerTokenResolver(Optional.empty()),
            true
        );

        assertThatThrownBy(() -> client.resolveSubmittedUserId(42L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Authenticated user lookup requires a Bearer token");
    }

    @Test
    void resolveSubmittedUserIdReturnsCurrentUserAndForwardsBearerToken() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        UserServiceClient client = newClient(Optional.of("user-token"), authorizationHeader, 200, "{\"id\":42}");

        assertThat(client.resolveSubmittedUserId(42L)).isEqualTo(42L);
        assertThat(authorizationHeader.get()).isEqualTo("Bearer user-token");
    }

    @Test
    void resolveSubmittedUserIdRejectsSubmittedUserMismatch() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        UserServiceClient client = newClient(Optional.of("user-token"), authorizationHeader, 200, "{\"id\":42}");

        assertThatThrownBy(() -> client.resolveSubmittedUserId(99L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Submitted user_id does not match the authenticated user");
    }

    @Test
    void resolveSubmittedUserIdMapsUserServiceFailuresToBadGateway() throws IOException {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        UserServiceClient client = newClient(Optional.of("user-token"), authorizationHeader, 500, "{\"message\":\"down\"}");

        assertThatThrownBy(() -> client.resolveSubmittedUserId(42L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Unable to validate current user");
    }

    private UserServiceClient newClient(
        Optional<String> bearerToken,
        AtomicReference<String> authorizationHeader,
        int status,
        String responseBody
    ) throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/users/me", exchange -> {
            requestCount.incrementAndGet();
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sendJson(exchange, status, responseBody);
        });
        server.start();

        return new UserServiceClient(
            RestClient.builder(),
            "http://localhost:" + server.getAddress().getPort(),
            new StubBearerTokenResolver(bearerToken),
            true
        );
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
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
