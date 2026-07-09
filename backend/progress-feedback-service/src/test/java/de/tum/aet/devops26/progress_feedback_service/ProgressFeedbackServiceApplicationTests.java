package de.tum.aet.devops26.progress_feedback_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:progress-feedback-service-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.flyway.enabled=false"
})
class ProgressFeedbackServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
