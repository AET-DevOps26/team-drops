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

    @Test
    void loadsEnglishSoftwareEngineeringSpeakingTrackWithThreeByThreeStructure() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey(
            "software-engineering-speaking",
            "English"
        );

        assertThat(template.title()).isEqualTo("Software Engineering Interview Speaking Practice");
        assertThat(template.lessons()).hasSize(3);
        assertThat(template.lessons()).allSatisfy(lesson -> assertThat(lesson.exercises()).hasSize(3));
        assertThat(template.lessons())
            .extracting("title")
            .containsExactly(
                "Introduction and Motivation",
                "Projects and Problem Solving",
                "Engineering Practices and Collaboration"
            );
        assertThat(template.lessons().stream().flatMap(lesson -> lesson.exercises().stream()))
            .hasSize(9)
            .allSatisfy(exercise -> {
                assertThat(exercise.subtype()).isEqualTo("speaking_prompt");
                assertThat(exercise.expectedAnswer()).isNotBlank();
                assertThat(exercise.keywords()).isNotEmpty();
            });
    }

    @Test
    void loadsGermanSoftwareEngineeringSpeakingTrackWithLocalizedExercises() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());

        DefaultLearningPlanContent template = catalog.findLocalizedByKey(
            "software-engineering-speaking",
            "German"
        );

        assertThat(template.title()).isEqualTo("Sprechtraining für Software-Engineering-Interviews");
        assertThat(template.defaultLanguage()).isEqualTo("German");
        assertThat(template.duration()).isEqualTo("1 Woche");
        assertThat(template.lessons()).hasSize(3);
        assertThat(template.lessons())
            .extracting("title")
            .containsExactly(
                "Vorstellung und Motivation",
                "Projekte und Problemlösung",
                "Engineering-Praktiken und Zusammenarbeit"
            );
        assertThat(template.lessons().stream().flatMap(lesson -> lesson.exercises().stream()))
            .hasSize(9)
            .allSatisfy(exercise -> {
                assertThat(exercise.subtype()).isEqualTo("speaking_prompt");
                assertThat(exercise.question()).isNotBlank();
                assertThat(exercise.expectedAnswer()).isNotBlank();
                assertThat(exercise.keywords()).isNotEmpty();
            });
    }
}
