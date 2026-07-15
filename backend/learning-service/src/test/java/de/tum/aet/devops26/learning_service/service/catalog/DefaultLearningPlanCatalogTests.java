package de.tum.aet.devops26.learning_service.service.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DefaultLearningPlanCatalogTests {

    @Test
    void loadsJobInterviewTemplateFromJsonResource() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey("job-interview", "English");

        assertThat(template.title()).isEqualTo("Job Interview Preparation");
        assertThat(template.lessons()).hasSize(5);
        assertThat(template.lessons().get(0).title()).isEqualTo("Self Introduction");
        assertThat(template.lessons().get(0).exercises())
            .extracting("question")
            .containsExactly(
                "Tell me about yourself.",
                "Write a short professional introduction.",
                "Improve your introduction using more formal vocabulary."
            );
    }

    @Test
    void loadsGermanJobInterviewTemplateFromJsonResource() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey("job-interview", "German");

        assertThat(template.title()).isEqualTo("Vorbereitung auf Vorstellungsgespräche");
        assertThat(template.defaultLanguage()).isEqualTo("German");
        assertThat(template.lessons().get(0).title()).isEqualTo("Selbstvorstellung");
        assertThat(template.lessons().get(0).exercises())
            .extracting("question")
            .containsExactly(
                "Erzählen Sie mir etwas über sich.",
                "Schreibe eine kurze professionelle Selbstvorstellung.",
                "Verbessere deine Vorstellung mit formellerem Wortschatz."
            );
    }

    @Test
    void fallsBackToEnglishWhenLanguageIsUnsupported() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey("job-interview", "Italian");

        assertThat(template.title()).isEqualTo("Job Interview Preparation");
    }

    @Test
    void loadsSoftwareEngineeringInterviewTemplateWithPremadeQuestions() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey("software-engineering-interview", "English");

        assertThat(template.title()).isEqualTo("Software Engineering Interview Track");
        assertThat(template.defaultGoal()).isEqualTo("Prepare for software engineering interviews");
        assertThat(template.lessons()).hasSize(8);
        assertThat(template.lessons().get(0).title()).isEqualTo("Introduction and Role Fit");
        assertThat(template.lessons().get(0).exercises())
            .extracting("question")
            .containsExactly(
                "Tell me about yourself as a software engineering candidate.",
                "Why are you interested in this software engineering role?",
                "What kind of engineering problems do you enjoy solving?"
            );
        assertThat(template.lessons().get(3).exercises().get(0).question())
            .isEqualTo("How would you design a simple URL shortener?");
        assertThat(template.lessons().get(3).exercises().get(0).keywords())
            .containsExactly(
                "requirements",
                "API",
                "database",
                "short code",
                "scalability"
            );
    }

    @Test
    void loadsGermanSoftwareEngineeringInterviewTemplateWithPremadeQuestions() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey("software-engineering-interview", "German");

        assertThat(template.title()).isEqualTo("Vorbereitung auf Software-Engineering-Interviews");
        assertThat(template.defaultLanguage()).isEqualTo("German");
        assertThat(template.lessons()).hasSize(8);
        assertThat(template.lessons().get(0).title()).isEqualTo("Vorstellung und Rollenpassung");
        assertThat(template.lessons().get(0).exercises().get(0).question())
            .isEqualTo("Erzählen Sie mir etwas über sich als Kandidatin oder Kandidat im Software Engineering.");
        assertThat(template.lessons().get(3).exercises().get(0).keywords())
            .containsExactly(
                "Anforderungen",
                "API",
                "Datenbank",
                "Kurzcode",
                "Skalierbarkeit"
            );
    }

    @Test
    void loadsMachineLearningInterviewTemplateWithKeywordHints() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey("machine-learning-interview", "English");

        assertThat(template.title()).isEqualTo("Machine Learning Interview Track");
        assertThat(template.lessons()).hasSize(10);
        assertThat(template.lessons().get(0).exercises().get(0).question())
            .isEqualTo("Describe a machine learning project you worked on.");
        assertThat(template.lessons().get(0).exercises().get(0).keywords())
            .containsExactly(
                "problem",
                "dataset",
                "preprocessing",
                "model",
                "evaluation metric",
                "result",
                "contribution",
                "concrete outcome"
            );
    }

    @Test
    void loadsGermanMachineLearningInterviewTemplateWithKeywordHints() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey("machine-learning-interview", "German");

        assertThat(template.title()).isEqualTo("Vorbereitung auf Machine-Learning-Interviews");
        assertThat(template.defaultLanguage()).isEqualTo("German");
        assertThat(template.lessons()).hasSize(10);
        assertThat(template.lessons().get(0).title()).isEqualTo("Ein ML-Projekt vorstellen");
        assertThat(template.lessons().get(0).exercises().get(0).question())
            .isEqualTo("Beschreibe ein Machine-Learning-Projekt, an dem du gearbeitet hast.");
        assertThat(template.lessons().get(0).exercises().get(0).keywords())
            .containsExactly(
                "Problem",
                "Datensatz",
                "Vorverarbeitung",
                "Modell",
                "Evaluationsmetrik",
                "Ergebnis",
                "Beitrag",
                "konkreter Effekt"
            );
    }
}
