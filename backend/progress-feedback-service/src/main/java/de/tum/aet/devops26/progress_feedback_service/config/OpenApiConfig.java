package de.tum.aet.devops26.progress_feedback_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI progressFeedbackServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Progress & Feedback Service API")
                        .version("1.0.0"))
                .addServersItem(new Server()
                        .url("http://localhost:8083")
                        .description("Progress-Feedback Service"));
    }
}
