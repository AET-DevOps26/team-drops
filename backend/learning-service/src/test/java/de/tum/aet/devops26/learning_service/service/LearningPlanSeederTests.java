package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.ExerciseSubtype;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanCatalog;
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

    @Mock
    private DefaultLearningPlanCatalog defaultLearningPlanCatalog;

    @Test
    void createSpeakingPlanSeedsExactlyThreeSoftwareInterviewLessonsWithThreeExercisesEach() throws Exception {
        DefaultLearningPlanCatalog catalog = new DefaultLearningPlanCatalog(new ObjectMapper());
        LearningPlanSeeder seeder = new LearningPlanSeeder(
            learningPlanRepository,
            lessonService,
            exerciseService,
            catalog
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
        assertThat(plan.getDescription()).isEqualTo(
            "Spoken practice for common software engineering interview questions."
        );
        assertThat(plan.getGoal()).isEqualTo("Improve spoken answers for software engineering interviews");
        assertThat(plan.getDuration()).isEqualTo("1 week");
        assertThat(plan.getLanguage()).isEqualTo("English");
        assertThat(plan.getLevel()).isEqualTo("A2");
        assertThat(plan.getStatus()).isEqualTo(LearningStatus.NOT_STARTED.getValue());
        assertThat(savedLessons)
            .extracting(Lesson::getTitle)
            .containsExactly(
                "Introduction and Motivation",
                "Projects and Problem Solving",
                "Engineering Practices and Collaboration"
            );
        assertThat(savedExercises).hasSize(9);
        assertThat(savedLessons).allSatisfy(lesson ->
            assertThat(savedExercises)
                .filteredOn(exercise -> exercise.getLessonId().equals(lesson.getId()))
                .hasSize(3)
        );
        assertThat(savedExercises).allSatisfy(exercise -> {
            assertThat(exercise.getType()).isEqualTo(ExerciseSubtype.SPEAKING_PROMPT.getValue());
            assertThat(exercise.getQuestion()).isNotBlank();
            assertThat(exercise.getExpectedAnswer()).isNotBlank();
            assertThat(exercise.getDifficulty()).isEqualTo(plan.getLevel());
        });
        assertThat(savedExercises)
            .extracting(Exercise::getQuestion)
            .contains(
                "Record your answer to: Describe a software project you are proud of.",
                "Record your answer to: What makes a REST API easy for other developers to use?",
                "Record your answer to: Tell me about a technical disagreement with a teammate."
            );
    }
}
