package de.tum.aet.devops26.learning_service.service.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class DefaultLearningPlanCatalog {

    private static final String CATALOG_RESOURCE = "default-learning-plans.json";

    private final List<DefaultLearningPlanTemplate> templates;

    public DefaultLearningPlanCatalog(ObjectMapper objectMapper) throws IOException {
        try (InputStream inputStream = new ClassPathResource(CATALOG_RESOURCE).getInputStream()) {
            this.templates = objectMapper.readValue(
                inputStream,
                new TypeReference<List<DefaultLearningPlanTemplate>>() {
                }
            );
        }
    }

    public DefaultLearningPlanTemplate findByKey(String key) {
        return templates.stream()
            .filter(template -> template.key().equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing default learning plan template: " + key));
    }
}
