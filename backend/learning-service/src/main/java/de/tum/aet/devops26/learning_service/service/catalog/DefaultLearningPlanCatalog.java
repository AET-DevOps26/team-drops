package de.tum.aet.devops26.learning_service.service.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class DefaultLearningPlanCatalog {

    private static final String CATALOG_RESOURCE = "default-learning-plans.json";
    private static final String FALLBACK_LANGUAGE = "English";

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

    public DefaultLearningPlanContent findLocalizedByKey(String key, String language) {
        DefaultLearningPlanTemplate template = findByKey(key);
        return template.languages().getOrDefault(
            normalizeLanguage(language),
            template.languages().get(FALLBACK_LANGUAGE)
        );
    }

    public DefaultLearningPlanContent findFallbackByKey(String key) {
        return findLocalizedByKey(key, FALLBACK_LANGUAGE);
    }

    public List<String> templateKeys() {
        return templates.stream()
            .map(DefaultLearningPlanTemplate::key)
            .toList();
    }

    public Optional<String> findKeyByLocalizedTitle(String title) {
        if (title == null) {
            return Optional.empty();
        }

        return templates.stream()
            .filter(template -> template.languages().values().stream()
                .anyMatch(content -> title.equals(content.title())))
            .map(DefaultLearningPlanTemplate::key)
            .findFirst();
    }

    public boolean hasLocalizedTitle(String key, String title) {
        if (title == null) {
            return false;
        }

        return findByKey(key).languages().values().stream()
            .anyMatch(content -> title.equals(content.title()));
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return FALLBACK_LANGUAGE;
        }

        return templates.stream()
            .flatMap(template -> template.languages().keySet().stream())
            .filter(candidate -> candidate.equalsIgnoreCase(language.trim()))
            .findFirst()
            .orElse(FALLBACK_LANGUAGE);
    }
}
