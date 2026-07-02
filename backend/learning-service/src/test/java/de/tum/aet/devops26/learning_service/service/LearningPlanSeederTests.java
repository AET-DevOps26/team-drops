package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.ExerciseSubtype;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningPlanSeederTests {

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private ExerciseService exerciseService;

    @Test
    void createSpeakingPlanSeedsGermanA2SpeakingLessonsAndExercises() {
        LearningPlanSeeder seeder = new LearningPlanSeeder(
            learningPlanRepository,
            lessonService,
            exerciseService
        );
        CreateDefaultLearningPlanRequest request = new CreateDefaultLearningPlanRequest()
            .userId(42L);
        List<Lesson> savedLessons = new ArrayList<>();
        List<Exercise> savedExercises = new ArrayList<>();
        AtomicLong lessonIds = new AtomicLong(100L);

        when(learningPlanRepository.save(any(LearningPlan.class))).thenAnswer(invocation -> {
            LearningPlan plan = invocation.getArgument(0);
            plan.setId(7L);
            return plan;
        });
        when(lessonService.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson lesson = invocation.getArgument(0);
            lesson.setId(lessonIds.getAndIncrement());
            savedLessons.add(lesson);
            return lesson;
        });
        when(exerciseService.save(any(Exercise.class))).thenAnswer(invocation -> {
            Exercise exercise = invocation.getArgument(0);
            savedExercises.add(exercise);
            return exercise;
        });

        LearningPlan plan = seeder.createSpeakingPlan(request);

        assertThat(plan.getTitle()).isEqualTo(LearningPlanSeeder.SPEAKING_TITLE);
        assertThat(plan.getLanguage()).isEqualTo("German");
        assertThat(plan.getLevel()).isEqualTo("A2");
        assertThat(plan.getStatus()).isEqualTo(LearningStatus.NOT_STARTED.getValue());
        assertThat(savedLessons).hasSizeGreaterThanOrEqualTo(3);
        assertThat(savedExercises).hasSizeGreaterThanOrEqualTo(savedLessons.size());
        assertThat(savedExercises).allSatisfy(exercise -> {
            assertThat(exercise.getType()).isEqualTo(ExerciseSubtype.SPEAKING_PROMPT.getValue());
            assertThat(exercise.getQuestion()).isNotBlank();
            assertThat(exercise.getExpectedAnswer()).isNotBlank();
            assertThat(exercise.getDifficulty()).isEqualTo(plan.getLevel());
        });
    }
}
