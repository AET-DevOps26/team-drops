package de.tum.aet.devops26.user_service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.aet.devops26.user_service.dto.UserResponse;
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
    "app.auth.enabled=false"
})
class UserServiceE2ETests {

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
    void createsAndReadsAUserThroughHttpAndPostgres() {
        ResponseEntity<UserResponse> currentUser = restTemplate.getForEntity(
            "/api/v1/users/me",
            UserResponse.class
        );

        assertThat(currentUser.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentUser.getBody()).isNotNull();
        assertThat(currentUser.getBody().getId()).isNotNull();

        ResponseEntity<UserResponse> persistedUser = restTemplate.getForEntity(
            "/api/v1/users/" + currentUser.getBody().getId(),
            UserResponse.class
        );

        assertThat(persistedUser.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(persistedUser.getBody()).isNotNull();
        assertThat(persistedUser.getBody().getEmail()).isEqualTo("local-dev@example.com");
        assertThat(persistedUser.getBody().getId()).isEqualTo(currentUser.getBody().getId());
    }
}
