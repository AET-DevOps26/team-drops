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
    void createListeningPlanSeedsSoftwareEngineeringInterviewLessonsAndExercises() {
        LearningPlanSeeder seeder = new LearningPlanSeeder(
            learningPlanRepository,
            lessonService,
            exerciseService,
            defaultLearningPlanCatalog
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

        LearningPlan plan = seeder.createListeningPlan(request);

        assertThat(plan.getTitle()).isEqualTo(LearningPlanSeeder.LISTENING_TITLE);
        assertThat(plan.getGoal()).isEqualTo("Improve listening comprehension for software engineering interviews");
        assertThat(plan.getLanguage()).isEqualTo("German");
        assertThat(plan.getLevel()).isEqualTo("A2");
        assertThat(plan.getStatus()).isEqualTo(LearningStatus.NOT_STARTED.getValue());
        assertThat(savedLessons)
            .extracting(Lesson::getTitle)
            .containsExactly(
                "Interview Self Introduction",
                "Project Deep Dive",
                "Debugging Discussion",
                "System Design Conversation",
                "API and Database Interview",
                "Testing and Code Review",
                "Team Collaboration",
                "Prioritization and Ownership"
            );
        assertThat(savedExercises)
            .extracting(Exercise::getQuestion)
            .containsExactly(
                "AI listening exercise 1: software engineering interview self-introduction",
                "AI listening exercise 1: software project interview explanation",
                "AI listening exercise 1: debugging interview answer",
                "AI listening exercise 1: software system design interview",
                "AI listening exercise 1: API and database interview tradeoffs",
                "AI listening exercise 1: testing and code review interview",
                "AI listening exercise 1: software team collaboration interview",
                "AI listening exercise 1: engineering prioritization and ownership interview"
            );
        assertThat(savedExercises).allSatisfy(exercise -> {
            assertThat(exercise.getType()).isEqualTo(ExerciseSubtype.LISTENING_CHOICE.getValue());
            assertThat(exercise.getExpectedAnswer()).isEqualTo("Select the most accurate listening response.");
            assertThat(exercise.getDifficulty()).isEqualTo(plan.getLevel());
        });
    }

    @Test
    void createSpeakingPlanSeedsSoftwareEngineeringInterviewLessonsAndExercises() {
        LearningPlanSeeder seeder = new LearningPlanSeeder(
            learningPlanRepository,
            lessonService,
            exerciseService,
            defaultLearningPlanCatalog
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
        assertThat(plan.getGoal()).isEqualTo("Improve spoken answers for software engineering interviews");
        assertThat(plan.getLanguage()).isEqualTo("German");
        assertThat(plan.getLevel()).isEqualTo("A2");
        assertThat(plan.getStatus()).isEqualTo(LearningStatus.NOT_STARTED.getValue());
        assertThat(savedLessons)
            .extracting(Lesson::getTitle)
            .containsExactly(
                "Self Introduction",
                "Project Deep Dive",
                "Debugging Story",
                "System Design Tradeoffs",
                "API Design",
                "Database Choice",
                "Testing and Review",
                "Prioritization"
            );
        assertThat(savedExercises)
            .extracting(Exercise::getQuestion)
            .containsExactly(
                "Record a 45-second answer to: Tell me about yourself as a software engineering candidate.",
                "Record how you would answer: Describe a software project you are proud of.",
                "Record how you would answer: Tell me about a difficult bug you fixed.",
                "Record how you would explain the difference between monoliths and microservices.",
                "Record how you would answer: What makes a REST API easy for other developers to use?",
                "Record how you would answer: When would you choose a relational database instead of a document database?",
                "Record how you would answer: What do you look for when reviewing another engineer's pull request?",
                "Record how you would answer: How do you prioritize engineering tasks when everything feels important?"
            );
        assertThat(savedExercises).allSatisfy(exercise -> {
            assertThat(exercise.getType()).isEqualTo(ExerciseSubtype.SPEAKING_PROMPT.getValue());
            assertThat(exercise.getQuestion()).isNotBlank();
            assertThat(exercise.getExpectedAnswer()).isNotBlank();
            assertThat(exercise.getDifficulty()).isEqualTo(plan.getLevel());
        });
    }
}
