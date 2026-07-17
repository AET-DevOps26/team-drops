package de.tum.aet.devops26.user_service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.aet.devops26.user_service.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:user-service-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.auth.enabled=false"
})
class UserServiceApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void currentUserEndpointCreatesThenReusesTheLocalUser() {
        ResponseEntity<UserResponse> firstResponse = restTemplate.getForEntity(
            "/api/v1/users/me",
            UserResponse.class
        );
        ResponseEntity<UserResponse> secondResponse = restTemplate.getForEntity(
            "/api/v1/users/me",
            UserResponse.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.getBody()).isNotNull();
        assertThat(firstResponse.getBody().getEmail()).isEqualTo("local-dev@example.com");
        assertThat(firstResponse.getBody().getNewUser()).isTrue();

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondResponse.getBody()).isNotNull();
        assertThat(secondResponse.getBody().getId()).isEqualTo(firstResponse.getBody().getId());
        assertThat(secondResponse.getBody().getNewUser()).isFalse();
    }
}
