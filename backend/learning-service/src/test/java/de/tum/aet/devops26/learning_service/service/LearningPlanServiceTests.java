package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.learning_service.dto.CreateAiLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.ExerciseType;
import de.tum.aet.devops26.learning_service.dto.LearningPlanResponse;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.dto.LessonSummaryResponse;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient.RagExercise;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient.RagLearningPlanResponse;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient.RagLesson;
import de.tum.aet.devops26.learning_service.integration.UserServiceClient;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LearningPlanServiceTests {

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private ExerciseService exerciseService;

    @Mock
    private LearningPlanSeeder learningPlanSeeder;

    @Mock
    private GenAiRagLearningPlanClient genAiRagLearningPlanClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Test
    void createDefaultLearningPlanReturnsExistingDefaultPlan() {
        LearningPlanService service = newService();
        CreateDefaultLearningPlanRequest request = request();
        LearningPlan existingPlan = fixedPlan(7L, LearningPlanSeeder.DEFAULT_TITLE);
        LearningPlan listeningPlan = fixedPlan(8L, LearningPlanSeeder.LISTENING_TITLE);
        LearningPlan speakingPlan = fixedPlan(9L, LearningPlanSeeder.SPEAKING_TITLE);

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.DEFAULT_TITLE))
            .thenReturn(Optional.of(existingPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.LISTENING_TITLE))
            .thenReturn(Optional.of(listeningPlan));
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.SPEAKING_TITLE))
            .thenReturn(Optional.of(speakingPlan));
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getTitle()).isEqualTo(LearningPlanSeeder.DEFAULT_TITLE);
        verify(learningPlanSeeder, never()).createDefaultPlan(any(CreateDefaultLearningPlanRequest.class));
        verify(learningPlanSeeder, never()).createListeningPlan(any(CreateDefaultLearningPlanRequest.class));
        verify(learningPlanSeeder, never()).createSpeakingPlan(any(CreateDefaultLearningPlanRequest.class));
        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
        verify(lessonService, never()).save(any(Lesson.class));
        verify(exerciseService, never()).save(any(Exercise.class));
    }

    @Test
    void createAiLearningPlanPersistsGeneratedRagPlanLessonsAndExercises() {
        LearningPlanService service = newService();
        configureGeneratedPlanSaves();
        CreateAiLearningPlanRequest request = aiRequest();
        when(userServiceClient.resolveSubmittedUserId(42L)).thenReturn(42L);
        when(genAiRagLearningPlanClient.generate(request)).thenReturn(generatedPlan());
        when(lessonService.findByPlanId(100L)).thenReturn(List.of(
            Lesson.builder()
                .id(201L)
                .planId(100L)
                .title("Interview Answers")
                .topic("STAR answers - Structure answers with examples.")
                .orderNumber(1)
                .build()
        ));
        when(lessonService.toSummaryResponse(any(Lesson.class))).thenReturn(new LessonSummaryResponse(
            201L,
            100L,
            "Interview Answers",
            "STAR answers - Structure answers with examples.",
            1,
            LearningStatus.NOT_STARTED,
            0,
            10,
            1
        ));

        LearningPlanResponse response = service.createAiLearningPlan(request);

        assertThat(response.getTitle()).isEqualTo("Generated German Interview Plan");
        assertThat(response.getLessons()).hasSize(1);

        ArgumentCaptor<LearningPlan> planCaptor = ArgumentCaptor.forClass(LearningPlan.class);
        verify(learningPlanRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().getTitle()).isEqualTo("Generated German Interview Plan");
        assertThat(planCaptor.getValue().getUserId()).isEqualTo(42L);
        assertThat(planCaptor.getValue().getDescription()).isEqualTo("Grounded RAG plan");
        assertThat(planCaptor.getValue().getGoal()).isEqualTo("Prepare for an interview");
        assertThat(planCaptor.getValue().getLanguage()).isEqualTo("German");
        assertThat(planCaptor.getValue().getLevel()).isEqualTo("B1");
        assertThat(planCaptor.getValue().getDuration()).isEqualTo("3 weeks");

        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonService).save(lessonCaptor.capture());
        assertThat(lessonCaptor.getValue().getTitle()).isEqualTo("Interview Answers");
        assertThat(lessonCaptor.getValue().getTopic()).isEqualTo("STAR answers - Structure answers with examples.");
        assertThat(lessonCaptor.getValue().getOrderNumber()).isEqualTo(1);
        verify(lessonService).saveContentBlocks(201L, List.of("Use situation, task, action, result."));

        ArgumentCaptor<Exercise> exerciseCaptor = ArgumentCaptor.forClass(Exercise.class);
        verify(exerciseService).save(exerciseCaptor.capture());
        assertThat(exerciseCaptor.getValue().getType()).isEqualTo("free_text");
        assertThat(exerciseCaptor.getValue().getQuestion()).isEqualTo("Write a STAR answer.");
        assertThat(exerciseCaptor.getValue().getExpectedAnswer()).isEqualTo("A structured answer.");
    }

    @Test
    void createAiLearningPlanRejectsInvalidLessonBoundsBeforeCallingGenAi() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest().minimumLessons(5).maximumLessons(2);

        assertThatThrownBy(() -> service.createAiLearningPlan(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("minimum_lessons must be less than or equal to maximum_lessons");

        verify(genAiRagLearningPlanClient, never()).generate(any(CreateAiLearningPlanRequest.class));
        verify(userServiceClient, never()).resolveSubmittedUserId(any());
        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
    }

    @Test
    void createAiLearningPlanRejectsOutOfRangeRequestsBeforeCallingGenAi() {
        LearningPlanService service = newService();

        assertBadRequestBeforeGenAi(service, aiRequest().durationWeeks(53), "duration_weeks must be between 1 and 52");
        assertBadRequestBeforeGenAi(service, aiRequest().studyHoursPerWeek(81), "study_hours_per_week must be between 1 and 80");
        assertBadRequestBeforeGenAi(service, aiRequest().minimumLessons(0), "minimum_lessons must be between 1 and 24");
        assertBadRequestBeforeGenAi(service, aiRequest().maximumLessons(25), "maximum_lessons must be between 1 and 24");
    }

    @Test
    void createAiLearningPlanRejectsMissingRequiredRequestFieldsBeforeCallingGenAi() {
        LearningPlanService service = newService();

        assertBadRequestBeforeGenAi(service, aiRequest().ragTopic(" "), "rag_topic is required");
        assertBadRequestBeforeGenAi(service, aiRequest().learningGoal(null), "learning_goal is required");
        assertBadRequestBeforeGenAi(service, aiRequest().targetLanguage(""), "target_language is required");
        assertBadRequestBeforeGenAi(service, aiRequest().currentLevel(null), "current_level is required");
        assertBadRequestBeforeGenAi(service, aiRequest().exerciseTypes(List.of()), "exercise_types must contain at least one value");
    }

    @Test
    void createAiLearningPlanRejectsMismatchedAuthenticatedUserBeforeCallingGenAi() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest().userId(99L);
        when(userServiceClient.resolveSubmittedUserId(99L)).thenThrow(new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Submitted user_id does not match the authenticated user."
        ));

        assertThatThrownBy(() -> service.createAiLearningPlan(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Submitted user_id does not match");

        verify(genAiRagLearningPlanClient, never()).generate(any(CreateAiLearningPlanRequest.class));
        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
        verify(lessonService, never()).save(any(Lesson.class));
        verify(exerciseService, never()).save(any(Exercise.class));
    }

    @Test
    void createAiLearningPlanDoesNotPersistWhenGenAiFails() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest();
        when(userServiceClient.resolveSubmittedUserId(42L)).thenReturn(42L);
        when(genAiRagLearningPlanClient.generate(request)).thenThrow(new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "GenAI unavailable"
        ));

        assertThatThrownBy(() -> service.createAiLearningPlan(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("GenAI unavailable");

        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
        verify(lessonService, never()).save(any(Lesson.class));
        verify(exerciseService, never()).save(any(Exercise.class));
    }

    @Test
    void createAiLearningPlanRejectsUnsupportedGenAiExerciseTypeBeforePersisting() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest();
        RagLearningPlanResponse plan = new RagLearningPlanResponse(
            "Generated German Interview Plan",
            "Grounded RAG plan",
            "Prepare for an interview",
            "German",
            "B1",
            "3 weeks",
            List.of(new RagLesson(
                "Interview Answers",
                "STAR answers",
                "Structure answers with examples.",
                1,
                List.of(),
                List.of(new RagExercise(
                    "grammar",
                    "free_text",
                    "Write a STAR answer.",
                    "A structured answer.",
                    "B1"
                ))
            )),
            List.of()
        );
        when(userServiceClient.resolveSubmittedUserId(42L)).thenReturn(42L);
        when(genAiRagLearningPlanClient.generate(request)).thenReturn(plan);

        assertThatThrownBy(() -> service.createAiLearningPlan(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("unsupported exercise type");

        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
        verify(lessonService, never()).save(any(Lesson.class));
        verify(exerciseService, never()).save(any(Exercise.class));
    }

    @Test
    void createAiLearningPlanRejectsGeneratedLessonCountBelowRequestedMinimumBeforePersisting() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest().minimumLessons(2).maximumLessons(4);

        assertGeneratedPlanRejectedBeforePersisting(
            service,
            request,
            generatedPlan(),
            "lesson count outside requested bounds"
        );
    }

    @Test
    void createAiLearningPlanRejectsGeneratedLessonCountAboveRequestedMaximumBeforePersisting() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest().minimumLessons(1).maximumLessons(1);
        RagLearningPlanResponse plan = generatedPlanWithLessons(
            generatedLesson(1, "Interview Answers"),
            generatedLesson(2, "Interview Follow-up")
        );

        assertGeneratedPlanRejectedBeforePersisting(
            service,
            request,
            plan,
            "lesson count outside requested bounds"
        );
    }

    @Test
    void createAiLearningPlanRejectsDuplicateGeneratedLessonOrderBeforePersisting() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest().minimumLessons(2).maximumLessons(4);
        RagLearningPlanResponse plan = generatedPlanWithLessons(
            generatedLesson(1, "Interview Answers"),
            generatedLesson(1, "Interview Follow-up")
        );

        assertGeneratedPlanRejectedBeforePersisting(
            service,
            request,
            plan,
            "duplicate, missing, or non-contiguous lesson order numbers"
        );
    }

    @Test
    void createAiLearningPlanRejectsGeneratedLessonOrderGapBeforePersisting() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest().minimumLessons(2).maximumLessons(4);
        RagLearningPlanResponse plan = generatedPlanWithLessons(
            generatedLesson(1, "Interview Answers"),
            generatedLesson(3, "Interview Follow-up")
        );

        assertGeneratedPlanRejectedBeforePersisting(
            service,
            request,
            plan,
            "duplicate, missing, or non-contiguous lesson order numbers"
        );
    }

    @Test
    void createAiLearningPlanRejectsUnrequestedGeneratedExerciseTypeBeforePersisting() {
        LearningPlanService service = newService();
        CreateAiLearningPlanRequest request = aiRequest().exerciseTypes(List.of(ExerciseType.WRITING));
        RagLearningPlanResponse plan = generatedPlanWithLessons(new RagLesson(
            "Speaking Practice",
            "Interview speaking",
            "Answer out loud.",
            1,
            List.of(),
            List.of(new RagExercise(
                "speaking",
                "speaking_prompt",
                "Answer this out loud.",
                "A spoken answer.",
                "B1"
            ))
        ));

        assertGeneratedPlanRejectedBeforePersisting(
            service,
            request,
            plan,
            "exercise type that was not requested"
        );
    }

    @Test
    void createDefaultLearningPlanReturnsDefaultPlanAndSeedsMissingListeningAndSpeakingPlans() {
        LearningPlanService service = newService();
        CreateDefaultLearningPlanRequest request = request();
        LearningPlan defaultPlan = fixedPlan(7L, LearningPlanSeeder.DEFAULT_TITLE);

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.DEFAULT_TITLE))
            .thenReturn(Optional.empty());
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.LISTENING_TITLE))
            .thenReturn(Optional.empty());
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.SPEAKING_TITLE))
            .thenReturn(Optional.empty());
        when(learningPlanSeeder.createDefaultPlan(request)).thenReturn(defaultPlan);
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getTitle()).isEqualTo(LearningPlanSeeder.DEFAULT_TITLE);
        verify(learningPlanSeeder).createDefaultPlan(request);
        verify(learningPlanSeeder).createListeningPlan(request);
        verify(learningPlanSeeder).createSpeakingPlan(request);
    }

    @Test
    void findResponsesByUserIdCreatesMissingFixedPlansForExistingUserBeforeReadingPlans() {
        LearningPlanService service = newService();
        LearningPlan existingPlan = fixedPlan(7L, "Custom Plan");

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.DEFAULT_TITLE))
            .thenReturn(Optional.empty());
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.LISTENING_TITLE))
            .thenReturn(Optional.empty());
        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, LearningPlanSeeder.SPEAKING_TITLE))
            .thenReturn(Optional.empty());
        when(learningPlanRepository.findByUserId(42L)).thenReturn(List.of(existingPlan));
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        List<LearningPlanResponse> responses = service.findResponsesByUserId(42L);

        assertThat(responses).extracting(LearningPlanResponse::getTitle).containsExactly("Custom Plan");
        verify(learningPlanSeeder).createDefaultPlan(any(CreateDefaultLearningPlanRequest.class));
        verify(learningPlanSeeder).createListeningPlan(any(CreateDefaultLearningPlanRequest.class));
        verify(learningPlanSeeder).createSpeakingPlan(any(CreateDefaultLearningPlanRequest.class));
    }

    private LearningPlanService newService() {
        return new LearningPlanService(
            learningPlanRepository,
            lessonService,
            exerciseService,
            learningPlanSeeder,
            genAiRagLearningPlanClient,
            userServiceClient
        );
    }

    private CreateDefaultLearningPlanRequest request() {
        return new CreateDefaultLearningPlanRequest()
            .userId(42L)
            .targetLanguage("German")
            .currentLevel("A2")
            .learningGoal("Prepare for a software engineering job interview");
    }

    private void assertBadRequestBeforeGenAi(
        LearningPlanService service,
        CreateAiLearningPlanRequest request,
        String expectedMessage
    ) {
        assertThatThrownBy(() -> service.createAiLearningPlan(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining(expectedMessage);

        verify(userServiceClient, never()).resolveSubmittedUserId(any());
        verify(genAiRagLearningPlanClient, never()).generate(any(CreateAiLearningPlanRequest.class));
        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
    }

    private void assertGeneratedPlanRejectedBeforePersisting(
        LearningPlanService service,
        CreateAiLearningPlanRequest request,
        RagLearningPlanResponse generatedPlan,
        String expectedMessage
    ) {
        when(userServiceClient.resolveSubmittedUserId(42L)).thenReturn(42L);
        when(genAiRagLearningPlanClient.generate(request)).thenReturn(generatedPlan);

        assertThatThrownBy(() -> service.createAiLearningPlan(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining(expectedMessage);

        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
        verify(lessonService, never()).save(any(Lesson.class));
        verify(exerciseService, never()).save(any(Exercise.class));
    }

    private LearningPlan fixedPlan(Long id, String title) {
        return LearningPlan.builder()
            .id(id)
            .userId(42L)
            .title(title)
            .description("Fixed plan")
            .goal("Practice")
            .language("German")
            .level("A2")
            .duration("1 week")
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build();
    }

    private void configureGeneratedPlanSaves() {
        when(learningPlanRepository.save(any(LearningPlan.class))).thenAnswer(invocation -> {
            LearningPlan plan = invocation.getArgument(0);
            plan.setId(100L);
            return plan;
        });
        when(lessonService.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson lesson = invocation.getArgument(0);
            lesson.setId(200L + lesson.getOrderNumber());
            return lesson;
        });
        when(exerciseService.save(any(Exercise.class))).thenAnswer(invocation -> {
            Exercise exercise = invocation.getArgument(0);
            exercise.setId(300L);
            return exercise;
        });
    }

    private CreateAiLearningPlanRequest aiRequest() {
        return new CreateAiLearningPlanRequest()
            .userId(42L)
            .ragTopic("job interview")
            .targetLanguage("German")
            .currentLevel("B1")
            .learningGoal("Prepare for an interview")
            .durationWeeks(3)
            .studyHoursPerWeek(4)
            .minimumLessons(1)
            .maximumLessons(2)
            .exerciseTypes(List.of(ExerciseType.WRITING));
    }

    private RagLearningPlanResponse generatedPlan() {
        return generatedPlanWithLessons(generatedLesson(1, "Interview Answers"));
    }

    private RagLearningPlanResponse generatedPlanWithLessons(RagLesson... lessons) {
        return new RagLearningPlanResponse(
            "Generated German Interview Plan",
            "Grounded RAG plan",
            "Prepare for an interview",
            "German",
            "B1",
            "3 weeks",
            List.of(lessons),
            List.of()
        );
    }

    private RagLesson generatedLesson(Integer orderNumber, String title) {
        return new RagLesson(
            title,
            "STAR answers",
            "Structure answers with examples.",
            orderNumber,
            List.of("Use situation, task, action, result."),
            List.of(new RagExercise(
                "writing",
                "free_text",
                "Write a STAR answer.",
                "A structured answer.",
                "B1"
            ))
        );
    }
}
