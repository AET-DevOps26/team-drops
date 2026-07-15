package de.tum.aet.devops26.progress_feedback_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
	"spring.datasource.url=jdbc:h2:mem:progress-feedback-service-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.flyway.enabled=false",
	"app.auth.enabled=true",
	"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://issuer.test",
	"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://issuer.test/jwks",
	"APP_VERSION=test-version"
})
class ProgressFeedbackServiceApplicationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void exposesPrometheusMetricsWithoutAuthentication() {
		ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getHeaders().getContentType().toString()).startsWith("text/plain");
		assertThat(response.getBody()).contains("application_info", "service=\"progress-feedback-service\"", "version=\"test-version\"");
	}

}
