package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.List;
import java.util.Optional;
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
    private LearningPlanSeeder learningPlanSeeder;

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
            learningPlanSeeder
        );
    }

    private CreateDefaultLearningPlanRequest request() {
        return new CreateDefaultLearningPlanRequest()
            .userId(42L)
            .targetLanguage("German")
            .currentLevel("A2")
            .learningGoal("Prepare for a software engineering job interview");
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
}
