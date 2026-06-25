package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.learning_service.dto.LessonResponse;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import de.tum.aet.devops26.learning_service.repository.LessonRepository;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanCatalog;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanContent;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLessonTemplate;
import de.tum.aet.devops26.learning_service.service.catalog.LocalizedExercise;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonServiceTests {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ExerciseService exerciseService;

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private DefaultLearningPlanCatalog defaultLearningPlanCatalog;

    private LessonService service;

    @BeforeEach
    void setUp() {
        service = new LessonService(
            lessonRepository,
            exerciseService,
            learningPlanRepository,
            defaultLearningPlanCatalog
        );
    }

    @Test
    void findResponseByIdReturnsGermanDefaultLessonAndExercises() {
        Lesson lesson = Lesson.builder()
            .id(3L)
            .planId(7L)
            .title("Self Introduction")
            .topic("Introduce yourself professionally in an interview.")
            .orderNumber(1)
            .build();
        LearningPlan plan = LearningPlan.builder()
            .id(7L)
            .title("Job Interview Preparation")
            .build();
        List<Exercise> exercises = List.of(
            Exercise.builder()
                .id(11L)
                .lessonId(3L)
                .type("free_text")
                .question("Tell me about yourself.")
                .difficulty("A2")
                .expectedAnswer("Write a clear, professional answer using specific details and formal vocabulary.")
                .build(),
            Exercise.builder()
                .id(12L)
                .lessonId(3L)
                .type("free_text")
                .question("Write a short professional introduction.")
                .difficulty("A2")
                .expectedAnswer("Write a clear, professional answer using specific details and formal vocabulary.")
                .build()
        );
        DefaultLearningPlanContent germanTemplate = new DefaultLearningPlanContent(
            "Vorbereitung auf Vorstellungsgespräche",
            "Feste Lektionen zum Üben professioneller Antworten in Vorstellungsgesprächen.",
            "2 weeks",
            "Sich auf ein professionelles Vorstellungsgespräch vorbereiten",
            "German",
            "A2",
            "Verfasse eine klare, professionelle Antwort mit konkreten Details und formellem Wortschatz.",
            List.of(new DefaultLessonTemplate(
                "Selbstvorstellung",
                "Stelle dich in einem Vorstellungsgespräch professionell vor.",
                List.of(
                    "Erzählen Sie mir etwas über sich.",
                    "Schreibe eine kurze professionelle Selbstvorstellung."
                )
            ))
        );

        when(lessonRepository.findById(3L)).thenReturn(Optional.of(lesson));
        when(learningPlanRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(defaultLearningPlanCatalog.hasLocalizedTitle("job-interview", "Job Interview Preparation")).thenReturn(true);
        when(defaultLearningPlanCatalog.findLocalizedByKey(any(), any())).thenReturn(germanTemplate);
        when(exerciseService.findByLessonId(3L)).thenReturn(exercises);
        when(exerciseService.toResponse(any(Exercise.class), any(LocalizedExercise.class))).thenCallRealMethod();

        LessonResponse response = service.findResponseById(3L, "German").orElseThrow();

        assertThat(response.getTitle()).isEqualTo("Selbstvorstellung");
        assertThat(response.getTopic()).isEqualTo("Stelle dich in einem Vorstellungsgespräch professionell vor.");
        assertThat(response.getExercises()).extracting("question")
            .containsExactly(
                "Erzählen Sie mir etwas über sich.",
                "Schreibe eine kurze professionelle Selbstvorstellung."
            );
        assertThat(response.getExercises()).extracting("expectedAnswer")
            .containsOnly("Verfasse eine klare, professionelle Antwort mit konkreten Details und formellem Wortschatz.");
        assertThat(response.getExercises()).extracting("format")
            .containsOnly("Kurze schriftliche Antwort");
    }
}
