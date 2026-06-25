package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
                defaultLearningPlanCatalog);
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
                defaultLearningPlanCatalog);
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest()
                .userId(42L);
        List<Lesson> savedLessons = new ArrayList<>();

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Job Interview Preparation"))
                .thenReturn(Optional.empty());
        when(learningPlanRepository.save(any(LearningPlan.class))).thenAnswer(invocation -> {
            LearningPlan plan = invocation.getArgument(0);
            plan.setId(100L);
            return plan;
        });
        when(lessonService.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson lesson = invocation.getArgument(0);
            lesson.setId((long) savedLessons.size() + 1);
            savedLessons.add(lesson);
            return lesson;
        });
        when(lessonService.findByPlanId(100L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(100L);

        ArgumentCaptor<LearningPlan> planCaptor = ArgumentCaptor.forClass(LearningPlan.class);
        verify(learningPlanRepository).save(planCaptor.capture());
        LearningPlan savedPlan = planCaptor.getValue();
        assertThat(savedPlan.getTitle()).isEqualTo("Job Interview Preparation");
        assertThat(savedPlan.getDescription())
                .isEqualTo("Fixed lessons for practicing professional job interview answers.");
        assertThat(savedPlan.getGoal()).isEqualTo("Prepare for a professional job interview");
        assertThat(savedPlan.getLanguage()).isEqualTo("English");
        assertThat(savedPlan.getLevel()).isEqualTo("A2");
        assertThat(savedPlan.getDuration()).isEqualTo("2 weeks");

        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonService, times(5)).save(lessonCaptor.capture());
        assertThat(lessonCaptor.getAllValues())
                .extracting(Lesson::getTitle)
                .containsExactly(
                        "Self Introduction",
                        "Education and Background",
                        "Work Experience and Internships",
                        "Project Explanation",
                        "Strengths and Weaknesses");
        assertThat(lessonCaptor.getAllValues())
                .extracting(Lesson::getOrderNumber)
                .containsExactly(1, 2, 3, 4, 5);

        ArgumentCaptor<Exercise> exerciseCaptor = ArgumentCaptor.forClass(Exercise.class);
        verify(exerciseService, times(15)).save(exerciseCaptor.capture());
        assertThat(exerciseCaptor.getAllValues())
                .hasSize(15)
                .allSatisfy(exercise -> {
                    assertThat(exercise.getType()).isEqualTo("free_text");
                    assertThat(exercise.getDifficulty()).isEqualTo("A2");
                    assertThat(exercise.getExpectedAnswer())
                            .isEqualTo(
                                    "Write a clear, professional answer using specific details and formal vocabulary.");
                });
        assertThat(exerciseCaptor.getAllValues())
                .extracting(Exercise::getQuestion)
                .contains(
                        "Tell me about yourself.",
                        "Describe your degree and specialization.",
                        "Describe one project you worked on.");
    }

    @Test
    void findResponsesByUserIdReturnsLocalizedDefaultPlan() {
        LearningPlanService service = new LearningPlanService(
                learningPlanRepository,
                lessonService,
                exerciseService,
                defaultLearningPlanCatalog);
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
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        List<LearningPlanResponse> responses = service.findResponsesByUserId(42L, "German");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("Vorbereitung auf Vorstellungsgespräche");
        assertThat(responses.get(0).getLanguage()).isEqualTo("German");
    }
}
