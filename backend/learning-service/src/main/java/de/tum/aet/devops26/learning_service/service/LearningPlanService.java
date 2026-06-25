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
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanCatalog;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanContent;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLessonTemplate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningPlanService {

    private static final String DEFAULT_TEMPLATE_KEY = "job-interview";
    private static final String DEFAULT_EXERCISE_TYPE = "free_text";

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;
    private final DefaultLearningPlanCatalog defaultLearningPlanCatalog;

    @Transactional
    public LearningPlanResponse createDefaultLearningPlan(CreateDefaultLearningPlanRequest request) {
        DefaultLearningPlanContent fallbackTemplate = defaultLearningPlanCatalog.findFallbackByKey(DEFAULT_TEMPLATE_KEY);
        return learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), fallbackTemplate.title())
            .map(plan -> toResponse(plan, request.getTargetLanguage()))
            .orElseGet(() -> createFixedDefaultLearningPlan(request, fallbackTemplate));
    }

    private LearningPlanResponse createFixedDefaultLearningPlan(
        CreateDefaultLearningPlanRequest request,
        DefaultLearningPlanContent template
    ) {
        LearningPlan plan = learningPlanRepository.save(LearningPlan.builder()
            .userId(request.getUserId())
            .title(template.title())
            .description(template.description())
            .goal(valueOrDefault(request.getLearningGoal(), template.defaultGoal()))
            .language(valueOrDefault(request.getTargetLanguage(), template.defaultLanguage()))
            .level(valueOrDefault(request.getCurrentLevel(), template.defaultLevel()))
            .duration(template.duration())
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build());

        for (int lessonIndex = 0; lessonIndex < template.lessons().size(); lessonIndex++) {
            DefaultLessonTemplate lessonTemplate = template.lessons().get(lessonIndex);
            Lesson lesson = lessonService.save(Lesson.builder()
                .planId(plan.getId())
                .title(lessonTemplate.title())
                .topic(lessonTemplate.topic())
                .orderNumber(lessonIndex + 1)
                .build());

            for (String question : lessonTemplate.exercises()) {
                exerciseService.save(Exercise.builder()
                    .lessonId(lesson.getId())
                    .type(DEFAULT_EXERCISE_TYPE)
                    .question(question)
                    .difficulty(plan.getLevel())
                    .expectedAnswer(template.defaultExpectedAnswer())
                    .build());
            }
        }

        return toResponse(plan, request.getTargetLanguage());
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
        return findResponsesByUserId(userId, null);
    }

    @Transactional(readOnly = true)
    public List<LearningPlanResponse> findResponsesByUserId(Long userId, String language) {
        return learningPlanRepository.findByUserId(userId).stream()
            .map(plan -> toResponse(plan, language))
            .toList();
    }

    private LearningPlanResponse toResponse(LearningPlan plan) {
        return toResponse(plan, null);
    }

    private LearningPlanResponse toResponse(LearningPlan plan, String language) {
        DefaultLearningPlanContent localizedTemplate = isDefaultLearningPlan(plan)
            ? defaultLearningPlanCatalog.findLocalizedByKey(DEFAULT_TEMPLATE_KEY, language)
            : null;

        return new LearningPlanResponse(
            plan.getId(),
            plan.getUserId(),
            localizedTemplate == null ? plan.getTitle() : localizedTemplate.title(),
            localizedTemplate == null ? plan.getDescription() : localizedTemplate.description(),
            localizedTemplate == null ? plan.getGoal() : localizedTemplate.defaultGoal(),
            localizedTemplate == null ? plan.getLanguage() : localizedTemplate.defaultLanguage(),
            plan.getLevel(),
            localizedTemplate == null ? plan.getDuration() : localizedTemplate.duration(),
            toLearningStatus(plan.getStatus()),
            plan.getProgress(),
            lessonService.findByPlanId(plan.getId()).stream()
                .map(lesson -> lessonService.toSummaryResponse(lesson, language))
                .toList()
        );
    }

    private boolean isDefaultLearningPlan(LearningPlan plan) {
        return defaultLearningPlanCatalog.hasLocalizedTitle(DEFAULT_TEMPLATE_KEY, plan.getTitle());
    }

    private LearningStatus toLearningStatus(String value) {
        return value == null ? LearningStatus.NOT_STARTED : LearningStatus.fromValue(value);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
