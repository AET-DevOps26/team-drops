package de.tum.aet.devops26.learning_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:learning-service-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "app.auth.enabled=false"
})
class LearningServiceApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void learningPlanEndpointSeedsAndReturnsACompletePlanCatalog() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/learning-plans/user/42?language=English",
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .contains("Job Interview Preparation")
            .contains("Software Engineering Interview Speaking Practice")
            .contains("lessons")
            .contains("exercises");
    }
}
