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
                List.of(
                        new DefaultLessonTemplate(
                                "Self Introduction",
                                "Introduce yourself professionally in an interview.",
                                List.of(
                                        "Tell me about yourself.",
                                        "Write a short professional introduction.",
                                        "Improve your introduction using more formal vocabulary.")),
                        new DefaultLessonTemplate(
                                "Education and Background",
                                "Explain your studies, university, and academic background.",
                                List.of(
                                        "Describe your degree and specialization.",
                                        "Explain why you chose your field.",
                                        "Practice saying your graduation status clearly.")),
                        new DefaultLessonTemplate(
                                "Work Experience and Internships",
                                "Talk about previous internships, jobs, or projects.",
                                List.of(
                                        "Describe one internship or work experience.",
                                        "Explain your responsibilities.",
                                        "Mention what you learned from the experience.")),
                        new DefaultLessonTemplate(
                                "Project Explanation",
                                "Present a technical or academic project clearly.",
                                List.of(
                                        "Describe one project you worked on.",
                                        "Explain the problem, your solution, and your role.",
                                        "Simplify a technical explanation for a non-technical interviewer.")),
                        new DefaultLessonTemplate(
                                "Strengths and Weaknesses",
                                "Answer common HR questions about strengths and weaknesses.",
                                List.of(
                                        "Name two strengths with examples.",
                                        "Explain one weakness professionally.",
                                        "Rewrite weak answers into stronger interview answers."))));

        germanTemplate = new DefaultLearningPlanContent(
                "Vorbereitung auf Vorstellungsgespräche",
                "Feste Lektionen zum Üben professioneller Antworten in Vorstellungsgesprächen.",
                "2 weeks",
                "Sich auf ein professionelles Vorstellungsgespräch vorbereiten",
                "German",
                "A2",
                "Verfasse eine klare, professionelle Antwort mit konkreten Details und formellem Wortschatz.",
                List.of(
                        new DefaultLessonTemplate(
                                "Selbstvorstellung",
                                "Stelle dich in einem Vorstellungsgespräch professionell vor.",
                                List.of(
                                        "Erzählen Sie mir etwas über sich.",
                                        "Schreibe eine kurze professionelle Selbstvorstellung.",
                                        "Verbessere deine Vorstellung mit formellerem Wortschatz."))));

        lenient().when(defaultLearningPlanCatalog.findFallbackByKey("job-interview")).thenReturn(defaultTemplate);
        when(defaultLearningPlanCatalog.findLocalizedByKey(any(), any())).thenAnswer(invocation -> {
            String language = invocation.getArgument(1);
            return "German".equalsIgnoreCase(language) ? germanTemplate : defaultTemplate;
        });
        when(defaultLearningPlanCatalog.hasLocalizedTitle("job-interview", "Job Interview Preparation"))
                .thenReturn(true);
    }

    @Test
    void createDefaultLearningPlanReturnsExistingDefaultPlan() {
        LearningPlanService service = new LearningPlanService(
                learningPlanRepository,
                lessonService,
                exerciseService,
                defaultLearningPlanCatalog,
                learningPlanSeeder);
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest()
                .userId(42L)
                .targetLanguage("German")
                .currentLevel("A2")
                .learningGoal("Prepare for a software engineering job interview");
        LearningPlan existingPlan = LearningPlan.builder()
                .id(7L)
                .userId(42L)
                .title("Job Interview Preparation")
                .description("Fixed lessons for practicing professional job interview answers.")
                .goal("Prepare for a professional job interview")
                .language("German")
                .level("A2")
                .duration("2 weeks")
                .status(LearningStatus.NOT_STARTED.getValue())
                .progress(0)
                .build();

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
                learningPlanSeeder);
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest()
                .userId(42L);
        LearningPlan savedPlan = LearningPlan.builder()
                .id(100L)
                .userId(42L)
                .title("Job Interview Preparation")
                .description("Fixed lessons for practicing professional job interview answers.")
                .goal("Prepare for a professional job interview")
                .language("English")
                .level("A2")
                .duration("2 weeks")
                .status(LearningStatus.NOT_STARTED.getValue())
                .progress(0)
                .build();

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Everyday Listening Practice"))
                .thenReturn(Optional.empty());
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Job Interview Preparation"))
                .thenReturn(Optional.empty());
        when(learningPlanSeeder.createDefaultPlan(request)).thenReturn(savedPlan);
        when(lessonService.findByPlanId(100L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(100L);
        verify(learningPlanSeeder).createListeningPlan(request);
        verify(learningPlanSeeder).createDefaultPlan(request);
    }

    @Test
    void findResponsesByUserIdReturnsLocalizedDefaultPlan() {
        LearningPlanService service = new LearningPlanService(
                learningPlanRepository,
                lessonService,
                exerciseService,
                defaultLearningPlanCatalog,
                learningPlanSeeder);
        LearningPlan existingPlan = LearningPlan.builder()
                .id(7L)
                .userId(42L)
                .title("Job Interview Preparation")
                .description("Fixed lessons for practicing professional job interview answers.")
                .goal("Prepare for a professional job interview")
                .language("English")
                .level("A2")
                .duration("2 weeks")
                .status(LearningStatus.NOT_STARTED.getValue())
                .progress(0)
                .build();

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
}
