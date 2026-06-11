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

    private LearningPlanService service;

    @BeforeEach
    void setUp() {
        service = new LearningPlanService(
            learningPlanRepository,
            lessonService,
            exerciseService
        );
    }

    @Test
    void createDefaultLearningPlanReturnsExistingDefaultPlan() {
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest()
            .userId(42L)
            .targetLanguage("German")
            .currentLevel("A2")
            .learningGoal("Prepare for a software engineering job interview");
        LearningPlan existingPlan = LearningPlan.builder()
            .id(7L)
            .userId(42L)
            .title("Machine Learning Interview Track")
            .description("German interview practice for explaining machine learning projects, systems, and production work.")
            .goal("Prepare for machine learning interviews in German")
            .language("German")
            .level("B1")
            .duration("4 weeks")
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build();

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Machine Learning Interview Track"))
            .thenReturn(Optional.of(existingPlan));
        when(lessonService.findByPlanId(7L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getId()).isEqualTo(7L);
        verify(learningPlanRepository, never()).save(any(LearningPlan.class));
        verify(lessonService, never()).save(any(Lesson.class));
        verify(exerciseService, never()).save(any(Exercise.class));
    }

    @Test
    void createDefaultLearningPlanCreatesMachineLearningInterviewTrack() {
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest()
            .userId(42L)
            .targetLanguage("German")
            .currentLevel("B1")
            .learningGoal("Prepare for ML interviews");

        when(learningPlanRepository.findFirstByUserIdAndTitle(42L, "Machine Learning Interview Track"))
            .thenReturn(Optional.empty());
        when(learningPlanRepository.save(any(LearningPlan.class))).thenAnswer(invocation -> {
            LearningPlan plan = invocation.getArgument(0);
            plan.setId(99L);
            return plan;
        });
        when(lessonService.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson lesson = invocation.getArgument(0);
            lesson.setId(100L + lesson.getOrderNumber());
            return lesson;
        });
        when(exerciseService.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonService.findByPlanId(99L)).thenReturn(List.of());

        LearningPlanResponse response = service.createDefaultLearningPlan(request);

        assertThat(response.getTitle()).isEqualTo("Machine Learning Interview Track");
        assertThat(response.getLanguage()).isEqualTo("German");
        assertThat(response.getLevel()).isEqualTo("B1");
        assertThat(response.getDuration()).isEqualTo("4 weeks");

        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonService, org.mockito.Mockito.times(10)).save(lessonCaptor.capture());
        assertThat(lessonCaptor.getAllValues())
            .extracting(Lesson::getTitle)
            .containsExactly(
                "Presenting an ML Project",
                "Data Preparation",
                "Model Selection",
                "Training and Overfitting",
                "Model Evaluation",
                "Improving Model Performance",
                "Deployment and Production",
                "ML System Design",
                "Explaining ML to Non-Technical People",
                "ML Behavioral Questions"
            );

        ArgumentCaptor<Exercise> exerciseCaptor = ArgumentCaptor.forClass(Exercise.class);
        verify(exerciseService, org.mockito.Mockito.times(33)).save(exerciseCaptor.capture());
        assertThat(exerciseCaptor.getAllValues().getFirst().getQuestion())
            .isEqualTo("Describe a machine learning project you worked on.");
        assertThat(exerciseCaptor.getAllValues().getFirst().getExpectedAnswer())
            .contains("Answer in German");
        assertThat(exerciseCaptor.getAllValues().getLast().getQuestion())
            .isEqualTo("How do you stay updated with developments in machine learning?");
    }
}
