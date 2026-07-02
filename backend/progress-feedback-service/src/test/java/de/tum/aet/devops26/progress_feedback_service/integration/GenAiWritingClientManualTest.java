package de.tum.aet.devops26.progress_feedback_service.integration;

import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationRequest;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "services.genai.base-url=http://localhost:8084"
})
@Disabled("Manual live GenAI check; default Gradle tests use mocked GenAI client coverage.")
class GenAiWritingClientManualTest {

    @Autowired
    private GenAiWritingClient genAiWritingClient;

    @Test
    void shouldCallGenAiWritingEvaluation() {
        WritingEvaluationRequest request = new WritingEvaluationRequest(
                5L,
                4L,
                "writing",
                "Describe your education.",
                "I studied computer science.",
                "Ich habe Informatik studiert.",
                "German",
                "A1"
        );

        WritingEvaluationResponse response = genAiWritingClient.evaluate(request);

        assertThat(response).isNotNull();
        assertThat(response.message()).isNotBlank();
    }
}
