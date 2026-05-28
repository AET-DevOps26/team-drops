package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.CreateAiLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.ExerciseType;
import de.tum.aet.devops26.learning_service.dto.LearningPlanResponse;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningPlanService {

    private static final String DEFAULT_TITLE = "Job Interview Preparation";
    private static final String DEFAULT_DESCRIPTION = "A default plan for practicing job interview conversations.";
    private static final String DEFAULT_DURATION = "2 weeks";

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;

    @Transactional
    public LearningPlanResponse createDefaultLearningPlan(CreateDefaultLearningPlanRequest request) {
        LearningPlan plan = learningPlanRepository.save(LearningPlan.builder()
            .userId(request.getUserId())
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .goal(request.getLearningGoal())
            .language(request.getTargetLanguage())
            .level(request.getCurrentLevel())
            .duration(DEFAULT_DURATION)
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build());

        Lesson introductionLesson = lessonService.save(Lesson.builder()
            .planId(plan.getId())
            .title("Introducing Yourself")
            .topic("Interview introduction")
            .orderNumber(1)
            .build());

        exerciseService.save(Exercise.builder()
            .lessonId(introductionLesson.getId())
            .type("free_text")
            .question("Tell me about yourself.")
            .difficulty("beginner")
            .expectedAnswer("A concise summary of experience, strengths, and motivation.")
            .build());

        Lesson behavioralLesson = lessonService.save(Lesson.builder()
            .planId(plan.getId())
            .title("Behavioral Questions")
            .topic("STAR answers")
            .orderNumber(2)
            .build());

        exerciseService.save(Exercise.builder()
            .lessonId(behavioralLesson.getId())
            .type("free_text")
            .question("Describe a challenging project and how you handled it.")
            .difficulty("intermediate")
            .expectedAnswer("A structured STAR response covering situation, task, action, and result.")
            .build());

        return toResponse(plan);
    }

    @Transactional
    public LearningPlanResponse createAiLearningPlan(CreateAiLearningPlanRequest request) {
        LearningPlan plan = learningPlanRepository.save(LearningPlan.builder()
            .userId(request.getUserId())
            .title(request.getTargetLanguage() + " AI Learning Plan")
            .description("An AI-assisted learning plan tailored to the learner's goal.")
            .goal(request.getLearningGoal())
            .language(request.getTargetLanguage())
            .level(request.getCurrentLevel())
            .duration(request.getDurationWeeks() + " weeks")
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build());

        int lessonCount = Math.max(1, Math.min(request.getMinimumLessons(), request.getMaximumLessons()));
        List<ExerciseType> requestedExerciseTypes = request.getExerciseTypes();

        for (int lessonIndex = 0; lessonIndex < lessonCount; lessonIndex++) {
            Lesson lesson = lessonService.save(Lesson.builder()
                .planId(plan.getId())
                .title("AI Lesson " + (lessonIndex + 1))
                .topic(request.getLearningGoal())
                .orderNumber(lessonIndex + 1)
                .build());

            for (int exerciseIndex = 0; exerciseIndex < requestedExerciseTypes.size(); exerciseIndex++) {
                ExerciseType exerciseType = requestedExerciseTypes.get(exerciseIndex);
                exerciseService.save(Exercise.builder()
                    .lessonId(lesson.getId())
                    .type(exerciseService.defaultSubtypeFor(exerciseType).getValue())
                    .question(exerciseService.buildAiQuestion(lesson, exerciseType, request.getLearningGoal(), exerciseIndex + 1))
                    .difficulty(request.getCurrentLevel())
                    .expectedAnswer(exerciseService.defaultExpectedAnswer(exerciseType))
                    .build());
            }
        }

        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<LearningPlanResponse> findResponsesByUserId(Long userId) {
        return learningPlanRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    private LearningPlanResponse toResponse(LearningPlan plan) {
        return new LearningPlanResponse(
            plan.getId(),
            plan.getUserId(),
            plan.getTitle(),
            plan.getDescription(),
            plan.getGoal(),
            plan.getLanguage(),
            plan.getLevel(),
            plan.getDuration(),
            toLearningStatus(plan.getStatus()),
            plan.getProgress(),
            lessonService.findByPlanId(plan.getId()).stream()
                .map(lessonService::toSummaryResponse)
                .toList()
        );
    }

    private LearningStatus toLearningStatus(String value) {
        return value == null ? LearningStatus.NOT_STARTED : LearningStatus.fromValue(value);
    }
}
