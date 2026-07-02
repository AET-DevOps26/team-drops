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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LearningPlanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LearningPlanService.class);

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;
    private final LearningPlanSeeder learningPlanSeeder;

    /**
     * Ensures the fixed plans exist for the user. Each plan is created in its own
     * REQUIRES_NEW transaction so a concurrent insert rolls back only that plan.
     */
    public LearningPlanResponse createDefaultLearningPlan(CreateDefaultLearningPlanRequest request) {
        ensureListeningPlan(request);
        ensureSpeakingPlan(request);

        return learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), LearningPlanSeeder.DEFAULT_TITLE)
            .map(this::toResponse)
            .orElseGet(() -> toResponse(createDefaultPlan(request)));
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

    @Transactional
    public List<LearningPlanResponse> findResponsesByUserId(Long userId) {
        ensureFixedPlans(new CreateDefaultLearningPlanRequest().userId(userId));
        return learningPlanRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    private void ensureFixedPlans(CreateDefaultLearningPlanRequest request) {
        ensureDefaultPlan(request);
        ensureListeningPlan(request);
        ensureSpeakingPlan(request);
    }

    private void ensureDefaultPlan(CreateDefaultLearningPlanRequest request) {
        if (learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), LearningPlanSeeder.DEFAULT_TITLE).isPresent()) {
            return;
        }
        createDefaultPlan(request);
    }

    private LearningPlan createDefaultPlan(CreateDefaultLearningPlanRequest request) {
        try {
            return learningPlanSeeder.createDefaultPlan(request);
        } catch (DataIntegrityViolationException exception) {
            LOGGER.info("Default plan already exists for user {} (concurrent creation)", request.getUserId());
            return learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), LearningPlanSeeder.DEFAULT_TITLE)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Default plan creation failed and no existing plan found for user " + request.getUserId()
                ));
        }
    }

    private void ensureListeningPlan(CreateDefaultLearningPlanRequest request) {
        if (learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), LearningPlanSeeder.LISTENING_TITLE).isPresent()) {
            return;
        }

        try {
            learningPlanSeeder.createListeningPlan(request);
        } catch (DataIntegrityViolationException exception) {
            LOGGER.info("Listening plan already exists for user {} (concurrent creation)", request.getUserId());
        }
    }

    private void ensureSpeakingPlan(CreateDefaultLearningPlanRequest request) {
        if (learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), LearningPlanSeeder.SPEAKING_TITLE).isPresent()) {
            return;
        }

        try {
            learningPlanSeeder.createSpeakingPlan(request);
        } catch (DataIntegrityViolationException exception) {
            LOGGER.info("Speaking plan already exists for user {} (concurrent creation)", request.getUserId());
        }
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
