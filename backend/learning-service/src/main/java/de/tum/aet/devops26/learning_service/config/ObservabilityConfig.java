package de.tum.aet.devops26.learning_service.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    public ObservabilityConfig(
        MeterRegistry registry,
        @Value("${spring.application.name}") String service,
        @Value("${APP_VERSION:unknown}") String version
    ) {
        Gauge.builder("application.info", () -> 1)
            .description("Deployed application version information.")
            .tag("service", service)
            .tag("version", version)
            .register(registry);
    }
}
