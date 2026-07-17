package de.tum.aet.devops26.progress_feedback_service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.aet.devops26.progress_feedback_service.dto.ProgressResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("e2e")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=false",
    "app.auth.enabled=false"
})
class ProgressFeedbackServiceE2ETests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsScopedProgressThroughHttpAndPostgres() {
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
