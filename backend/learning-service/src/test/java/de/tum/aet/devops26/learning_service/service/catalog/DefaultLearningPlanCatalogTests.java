package de.tum.aet.devops26.learning_service.service.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DefaultLearningPlanCatalogTests {

    @Test
    void loadsJobInterviewTemplateFromJsonResource() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanTemplate template = catalog.findByKey("job-interview");

        assertThat(template.title()).isEqualTo("Job Interview Preparation");
        assertThat(template.lessons()).hasSize(5);
        assertThat(template.lessons().get(0).title()).isEqualTo("Self Introduction");
        assertThat(template.lessons().get(0).exercises())
            .containsExactly(
                "Tell me about yourself.",
                "Write a short professional introduction.",
                "Improve your introduction using more formal vocabulary."
            );
    }
}
