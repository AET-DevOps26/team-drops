package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.LearningPlanResponse;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultExerciseTemplate;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanCatalog;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanContent;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLessonTemplate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningPlanServiceTests {

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private ExerciseService exerciseService;

    @Mock
    private DefaultLearningPlanCatalog defaultLearningPlanCatalog;

    @Mock
    private LearningPlanSeeder learningPlanSeeder;

    private DefaultLearningPlanContent defaultTemplate;
    private DefaultLearningPlanContent germanTemplate;

    @BeforeEach
    void setUp() {
        defaultTemplate = new DefaultLearningPlanContent(
            "Job Interview Preparation",
            "Fixed lessons for practicing professional job interview answers.",
            "2 weeks",
            "Prepare for a professional job interview",
            "English",
            "A2",
            "Write a clear, professional answer using specific details and formal vocabulary.",
            List.of(lesson(
                "Self Introduction",
                "Introduce yourself professionally in an interview.",
                "Tell me about yourself.",
                "Write a short professional introduction.",
                "Improve your introduction using more formal vocabulary."
            ))
        );

        germanTemplate = new DefaultLearningPlanContent(
            "Vorbereitung auf Vorstellungsgespräche",
            "Feste Lektionen zum Üben professioneller Antworten in Vorstellungsgesprächen.",
            "2 weeks",
            "Sich auf ein professionelles Vorstellungsgespräch vorbereiten",
            "German",
            "A2",
            "Verfasse eine klare, professionelle Antwort mit konkreten Details und formellem Wortschatz.",
            List.of(lesson(
                "Selbstvorstellung",
                "Stelle dich in einem Vorstellungsgespräch professionell vor.",
                "Erzählen Sie mir etwas über sich.",
                "Schreibe eine kurze professionelle Selbstvorstellung.",
                "Verbessere deine Vorstellung mit formellerem Wortschatz."
            ))
        );

        lenient().when(defaultLearningPlanCatalog.findFallbackByKey("job-interview")).thenReturn(defaultTemplate);
        lenient().when(defaultLearningPlanCatalog.templateKeys()).thenReturn(List.of("job-interview"));
        lenient().when(defaultLearningPlanCatalog.findKeyByLocalizedTitle("Job Interview Preparation"))
            .thenReturn(Optional.of("job-interview"));
        lenient().when(defaultLearningPlanCatalog.findLocalizedByKey(any(), any())).thenAnswer(invocation -> {
            String language = invocation.getArgument(1);
            return "German".equalsIgnoreCase(language) ? germanTemplate : defaultTemplate;
        });
    }

    @Test
    void createDefaultLearningPlanReturnsExistingDefaultPlan() {
        LearningPlanService service = new LearningPlanService(
            learningPlanRepository,
            lessonService,
            exerciseService,
            defaultLearningPlanCatalog,
            learningPlanSeeder
        );
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest()
            .userId(42L)
            .targetLanguage("German")
            .currentLevel("A2")
            .learningGoal("Prepare for a software engineering job interview");
        LearningPlan existingPlan = jobInterviewPlan(7L, 42L, "German");

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Job Interview Preparation"))
            .thenReturn(Optional.of(existingPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Everyday Listening Practice"))
            .thenReturn(Optional.of(existingPlan));
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getTitle()).isEqualTo("Vorbereitung auf Vorstellungsgespräche");
        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
        verify(lessonService, never()).save(any(Lesson.class));
        verify(exerciseService, never()).save(any(Exercise.class));
    }

    @Test
    void createDefaultLearningPlanCopiesCatalogLessonsAndExercises() {
        LearningPlanService service = new LearningPlanService(
            learningPlanRepository,
            lessonService,
            exerciseService,
            defaultLearningPlanCatalog,
            learningPlanSeeder
        );
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest().userId(42L);
        LearningPlan savedPlan = jobInterviewPlan(100L, 42L, "English");

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Everyday Listening Practice"))
            .thenReturn(Optional.empty());
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Job Interview Preparation"))
            .thenReturn(Optional.empty());
        when(learningPlanSeeder.createDefaultPlan(request, "job-interview")).thenReturn(savedPlan);
        when(lessonService.findByPlanId(100L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(100L);
        verify(learningPlanSeeder).createListeningPlan(request);
        verify(learningPlanSeeder).createDefaultPlan(request, "job-interview");
    }

    @Test
    void createDefaultLearningPlanAlsoSeedsAdditionalCatalogPlans() {
        LearningPlanService service = new LearningPlanService(
            learningPlanRepository,
            lessonService,
            exerciseService,
            defaultLearningPlanCatalog,
            learningPlanSeeder
        );
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest().userId(42L);
        LearningPlan existingPlan = jobInterviewPlan(7L, 42L, "English");
        DefaultLearningPlanContent mlTemplate = new DefaultLearningPlanContent(
            "Machine Learning Interview Track",
            "Fixed lessons for practicing machine learning interview answers in English.",
            "4 weeks",
            "Prepare for machine learning interviews in English",
            "English",
            "Intermediate",
            "Clear technical reasoning, concrete examples, and structured explanations.",
            List.of(lesson(
                "Presenting an ML Project",
                "Practice explaining an ML project.",
                "Describe a machine learning project you worked on."
            ))
        );

        when(defaultLearningPlanCatalog.templateKeys())
            .thenReturn(List.of("job-interview", "machine-learning-interview"));
        when(defaultLearningPlanCatalog.findFallbackByKey("machine-learning-interview")).thenReturn(mlTemplate);
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Everyday Listening Practice"))
            .thenReturn(Optional.of(existingPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Job Interview Preparation"))
            .thenReturn(Optional.of(existingPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Machine Learning Interview Track"))
            .thenReturn(Optional.empty());
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(7L);
        verify(learningPlanSeeder).createDefaultPlan(request, "machine-learning-interview");
    }

    @Test
    void findResponsesByUserIdReturnsLocalizedDefaultPlan() {
        LearningPlanService service = new LearningPlanService(
            learningPlanRepository,
            lessonService,
            exerciseService,
            defaultLearningPlanCatalog,
            learningPlanSeeder
        );
        LearningPlan existingPlan = jobInterviewPlan(7L, 42L, "English");

        when(learningPlanRepository.findByUserId(42L)).thenReturn(List.of(existingPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Everyday Listening Practice"))
            .thenReturn(Optional.of(existingPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Job Interview Preparation"))
            .thenReturn(Optional.of(existingPlan));
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        List<LearningPlanResponse> responses = service.findResponsesByUserId(42L, "German");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("Vorbereitung auf Vorstellungsgespräche");
        assertThat(responses.get(0).getLanguage()).isEqualTo("German");
    }

    @Test
    void findResponsesByUserIdReturnsLocalizedMachineLearningPlanTitle() {
        LearningPlanService service = new LearningPlanService(
            learningPlanRepository,
            lessonService,
            exerciseService,
            defaultLearningPlanCatalog,
            learningPlanSeeder
        );
        LearningPlan mlPlan = LearningPlan.builder()
            .id(8L)
            .userId(42L)
            .title("Machine Learning Interview Track")
            .description("Fixed lessons for practicing machine learning interview answers in English.")
            .goal("Prepare for machine learning interviews in English")
            .language("English")
            .level("Intermediate")
            .duration("4 weeks")
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build();
        DefaultLearningPlanContent germanMlTemplate = new DefaultLearningPlanContent(
            "Vorbereitung auf Machine-Learning-Interviews",
            "Feste Lektionen zum Üben von Antworten für Machine-Learning-Interviews auf Deutsch.",
            "4 weeks",
            "Sich gezielt auf Machine-Learning-Interviews auf Deutsch vorbereiten",
            "German",
            "Intermediate",
            "Klare technische Argumentation, konkrete Beispiele und strukturierte Erklärungen.",
            List.of(lesson(
                "Ein ML-Projekt vorstellen",
                "Übe, ein ML-Projekt klar und überzeugend zu erklären.",
                "Beschreibe ein Machine-Learning-Projekt, an dem du gearbeitet hast."
            ))
        );

        when(defaultLearningPlanCatalog.findKeyByLocalizedTitle("Machine Learning Interview Track"))
            .thenReturn(Optional.of("machine-learning-interview"));
        when(defaultLearningPlanCatalog.findLocalizedByKey("machine-learning-interview", "German"))
            .thenReturn(germanMlTemplate);
        when(learningPlanRepository.findByUserId(42L)).thenReturn(List.of(mlPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Everyday Listening Practice"))
            .thenReturn(Optional.of(mlPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Job Interview Preparation"))
            .thenReturn(Optional.of(mlPlan));
        when(lessonService.findByPlanId(8L)).thenReturn(List.of());

        List<LearningPlanResponse> responses = service.findResponsesByUserId(42L, "German");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("Vorbereitung auf Machine-Learning-Interviews");
        assertThat(responses.get(0).getLanguage()).isEqualTo("German");
    }

    private LearningPlan jobInterviewPlan(Long id, Long userId, String language) {
        return LearningPlan.builder()
            .id(id)
            .userId(userId)
            .title("Job Interview Preparation")
            .description("Fixed lessons for practicing professional job interview answers.")
            .goal("Prepare for a professional job interview")
            .language(language)
            .level("A2")
            .duration("2 weeks")
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build();
    }

    private DefaultLessonTemplate lesson(String title, String topic, String... questions) {
        return new DefaultLessonTemplate(
            title,
            topic,
            List.of(questions).stream()
                .map(question -> new DefaultExerciseTemplate(question, List.of()))
                .toList()
        );
    }
}
