package de.tum.aet.devops26.learning_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.tum.aet.devops26.learning_service.dto.CreateAiLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.ExerciseType;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient.RagLearningPlanResponse;
import de.tum.aet.devops26.learning_service.security.CurrentBearerTokenResolver;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GenAiRagLearningPlanClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateSendsLearningPlanPayloadAndParsesResponse() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiRagLearningPlanClient client = newClient(
            false,
            Optional.empty(),
            authorizationHeader,
            requestBody,
            200,
            successJson()
        );

        RagLearningPlanResponse response = client.generate(request());

        assertThat(response.title()).isEqualTo("German Interview Plan");
        assertThat(response.lessons()).hasSize(1);
        assertThat(response.lessons().getFirst().exercises().getFirst().subtype()).isEqualTo("free_text");
        assertThat(response.sources().getFirst().source()).isEqualTo("interview-guide.pdf");

        JsonNode body = requestBody.get();
        assertThat(body.get("topic").asText()).isEqualTo("job interview");
        assertThat(body.get("learning_goal").asText()).isEqualTo("Prepare for an interview");
        assertThat(body.get("target_language").asText()).isEqualTo("German");
        assertThat(body.get("level").asText()).isEqualTo("B1");
        assertThat(body.get("duration_weeks").asInt()).isEqualTo(3);
        assertThat(body.get("study_hours_per_week").asInt()).isEqualTo(4);
        assertThat(body.get("minimum_lessons").asInt()).isEqualTo(2);
        assertThat(body.get("maximum_lessons").asInt()).isEqualTo(4);
        assertThat(body.get("exercise_types").get(0).asText()).isEqualTo("writing");
        assertThat(body.get("exercise_types").get(1).asText()).isEqualTo("speaking");
        assertThat(authorizationHeader.get()).isNull();
    }

    @Test
    void generateForwardsAuthorizationHeaderWhenTokenExists() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiRagLearningPlanClient client = newClient(
            true,
            Optional.of("rag-token"),
            authorizationHeader,
            requestBody,
            200,
            successJson()
        );

        client.generate(request());

        assertThat(authorizationHeader.get()).isEqualTo("Bearer rag-token");
    }

    @Test
    void generateFailsClearlyWhenAuthIsEnabledAndTokenIsMissing() {
        GenAiRagLearningPlanClient client = new GenAiRagLearningPlanClient(
            objectMapper,
            "http://localhost:1",
            new StubBearerTokenResolver(Optional.empty()),
            true
        );

        assertThatThrownBy(() -> client.generate(request()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Authenticated RAG learning-plan generation requires a Bearer token");
    }

    @Test
    void generateMapsDownstreamErrorsToBadGateway() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        GenAiRagLearningPlanClient client = newClient(
            false,
            Optional.empty(),
            authorizationHeader,
            requestBody,
            502,
            "{\"message\":\"LLM unavailable\"}"
        );

        assertThatThrownBy(() -> client.generate(request()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("GenAI service rejected RAG learning-plan generation");
    }

    @Test
    void generateRejectsNullRequestBeforeCallingDownstream() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        GenAiRagLearningPlanClient client = newClient(
            false,
            Optional.empty(),
            new AtomicReference<>(),
            new AtomicReference<>(),
            200,
            successJson(),
            requestCount
        );

        assertThatThrownBy(() -> client.generate(null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("RAG learning-plan request must not be null");
        assertThat(requestCount).hasValue(0);
    }

    private GenAiRagLearningPlanClient newClient(
        boolean authEnabled,
        Optional<String> bearerToken,
        AtomicReference<String> authorizationHeader,
        AtomicReference<JsonNode> requestBody,
        int status,
        String responseBody
    ) throws IOException {
        return newClient(authEnabled, bearerToken, authorizationHeader, requestBody, status, responseBody, new AtomicInteger());
    }

    private GenAiRagLearningPlanClient newClient(
        boolean authEnabled,
        Optional<String> bearerToken,
        AtomicReference<String> authorizationHeader,
        AtomicReference<JsonNode> requestBody,
        int status,
        String responseBody,
        AtomicInteger requestCount
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/genai/rag/learning-plan", exchange -> {
            requestCount.incrementAndGet();
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(objectMapper.readTree(exchange.getRequestBody()));
            sendJson(exchange, status, responseBody);
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new GenAiRagLearningPlanClient(
            objectMapper,
            baseUrl,
            new StubBearerTokenResolver(bearerToken),
            authEnabled
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

    private String successJson() {
        return """
            {
              "title": "German Interview Plan",
              "description": "Prepare using grounded RAG content.",
              "goal": "Prepare for an interview",
              "language": "German",
              "level": "B1",
              "duration": "3 weeks",
              "lessons": [
                {
                  "title": "Interview Answers",
                  "topic": "STAR method",
                  "summary": "Structure answers with concrete examples.",
                  "order_number": 1,
                  "content_blocks": ["Use situation, task, action, result."],
                  "exercises": [
                    {
                      "type": "writing",
                      "subtype": "free_text",
                      "question": "Write a STAR answer.",
                      "expected_answer": "A structured answer.",
                      "difficulty": "B1"
                    }
                  ]
                }
              ],
              "sources": [
                {
                  "source": "interview-guide.pdf",
                  "page": 5,
                  "chunk_index": 2,
                  "score": 0.87,
                  "text": "Use the STAR method."
                }
              ]
            }
            """;
    }

    private CreateAiLearningPlanRequest request() {
        return new CreateAiLearningPlanRequest()
            .userId(42L)
            .ragTopic("job interview")
            .targetLanguage("German")
            .currentLevel("B1")
            .learningGoal("Prepare for an interview")
            .durationWeeks(3)
            .studyHoursPerWeek(4)
            .minimumLessons(2)
            .maximumLessons(4)
            .exerciseTypes(List.of(ExerciseType.WRITING, ExerciseType.SPEAKING));
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
