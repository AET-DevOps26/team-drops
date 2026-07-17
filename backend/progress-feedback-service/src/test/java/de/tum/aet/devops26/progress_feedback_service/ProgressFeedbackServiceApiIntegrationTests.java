package de.tum.aet.devops26.progress_feedback_service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.aet.devops26.progress_feedback_service.dto.ProgressResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:progress-feedback-service-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "app.auth.enabled=false"
})
class ProgressFeedbackServiceApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void progressEndpointReturnsAStableEmptyAggregateForANewUser() {
        ResponseEntity<ProgressResponse> response = restTemplate.getForEntity(
            "/api/v1/progress/user/42?plan_id=7&target_language=German",
            ProgressResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(42L);
        assertThat(response.getBody().getCompletedExercises()).isZero();
        assertThat(response.getBody().getTotalExercises()).isZero();
        assertThat(response.getBody().getAverageScore()).isZero();
    }
}
