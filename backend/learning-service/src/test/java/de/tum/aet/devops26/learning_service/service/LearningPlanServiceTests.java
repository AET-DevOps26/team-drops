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
        LearningPlanService service = new LearningPlanService(
            learningPlanRepository,
            lessonService,
            exerciseService,
            learningPlanSeeder
        );
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
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(7L);
        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
        verify(lessonService, never()).save(any(Lesson.class));
        verify(exerciseService, never()).save(any(Exercise.class));
    }
}
