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

    private static final String PRIMARY_DEFAULT_TEMPLATE_KEY = "job-interview";
    private static final Logger LOGGER = LoggerFactory.getLogger(LearningPlanService.class);

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;
    private final DefaultLearningPlanCatalog defaultLearningPlanCatalog;
    private final LearningPlanSeeder learningPlanSeeder;

    @Transactional
    public LearningPlanResponse createDefaultLearningPlan(CreateDefaultLearningPlanRequest request) {
        DefaultLearningPlanContent fallbackTemplate = defaultLearningPlanCatalog
                .findFallbackByKey(PRIMARY_DEFAULT_TEMPLATE_KEY);

        ensureListeningPlan(request);
        ensureSpeakingPlan(request);
        ensureAdditionalCatalogPlans(request, PRIMARY_DEFAULT_TEMPLATE_KEY);

        try {
            return learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), fallbackTemplate.title())
                .map(plan -> toResponse(plan, request.getTargetLanguage()))
                .orElseGet(() -> toResponse(
                    learningPlanSeeder.createDefaultPlan(request, PRIMARY_DEFAULT_TEMPLATE_KEY),
                    request.getTargetLanguage()
                ));
        } catch (DataIntegrityViolationException e) {
            LOGGER.info("Default plan already exists for user {} (concurrent creation)", request.getUserId());
            return learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), fallbackTemplate.title())
                .map(plan -> toResponse(plan, request.getTargetLanguage()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Default plan creation failed and no existing plan found for user " + request.getUserId()));
        }
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
                        .question(exerciseService.buildAiQuestion(lesson, exerciseType, request.getLearningGoal(),
                                exerciseIndex + 1))
                        .difficulty(request.getCurrentLevel())
                        .expectedAnswer(exerciseService.defaultExpectedAnswer(exerciseType))
                        .build());
            }
        }

        return toResponse(plan);
    }

    @Transactional
    public List<LearningPlanResponse> findResponsesByUserId(Long userId) {
        return findResponsesByUserId(userId, null);
    }

    @Transactional
    public List<LearningPlanResponse> findResponsesByUserId(Long userId, String language) {
        ensureFixedPlans(new CreateDefaultLearningPlanRequest().userId(userId));
        return learningPlanRepository.findByUserId(userId).stream()
                .map(plan -> toResponse(plan, language))
                .toList();
    }

    private void ensureFixedPlans(CreateDefaultLearningPlanRequest request) {
        ensureDefaultPlan(request);
        ensureListeningPlan(request);
        ensureSpeakingPlan(request);
        ensureAdditionalCatalogPlans(request, null);
    }

    private void ensureAdditionalCatalogPlans(CreateDefaultLearningPlanRequest request, String excludedTemplateKey) {
        for (String templateKey : defaultLearningPlanCatalog.templateKeys()) {
            if (templateKey.equals(excludedTemplateKey)) {
                continue;
            }
            ensureCatalogPlan(request, templateKey);
        }
    }

    private void ensureCatalogPlan(CreateDefaultLearningPlanRequest request, String templateKey) {
        DefaultLearningPlanContent fallbackTemplate = defaultLearningPlanCatalog.findFallbackByKey(templateKey);
        try {
            if (learningPlanRepository.findFirstByUserIdAndTitle(
                    request.getUserId(), fallbackTemplate.title()).isEmpty()) {
                learningPlanSeeder.createDefaultPlan(request, templateKey);
            }
        } catch (DataIntegrityViolationException e) {
            LOGGER.info(
                "Default plan {} already exists for user {} (concurrent creation)",
                templateKey,
                request.getUserId()
            );
        }
    }

    private void ensureDefaultPlan(CreateDefaultLearningPlanRequest request) {
        DefaultLearningPlanContent fallbackTemplate = defaultLearningPlanCatalog.findFallbackByKey(PRIMARY_DEFAULT_TEMPLATE_KEY);
        if (learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), fallbackTemplate.title()).isPresent()) {
            return;
        }
        createDefaultPlan(request);
    }

    private LearningPlan createDefaultPlan(CreateDefaultLearningPlanRequest request) {
        try {
            return learningPlanSeeder.createDefaultPlan(request, PRIMARY_DEFAULT_TEMPLATE_KEY);
        } catch (DataIntegrityViolationException exception) {
            DefaultLearningPlanContent fallbackTemplate = defaultLearningPlanCatalog.findFallbackByKey(PRIMARY_DEFAULT_TEMPLATE_KEY);
            LOGGER.info("Default plan already exists for user {} (concurrent creation)", request.getUserId());
            return learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), fallbackTemplate.title())
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
        return toResponse(plan, null);
    }

    private LearningPlanResponse toResponse(LearningPlan plan, String language) {
        DefaultLearningPlanContent localizedTemplate = defaultLearningPlanCatalog.findKeyByLocalizedTitle(plan.getTitle())
                .map(templateKey -> defaultLearningPlanCatalog.findLocalizedByKey(templateKey, language))
                .orElse(null);

        return new LearningPlanResponse(
                plan.getId(),
                plan.getUserId(),
                localizedTemplate == null ? plan.getTitle() : localizedTemplate.title(),
                localizedTemplate == null ? plan.getDescription() : localizedTemplate.description(),
                plan.getGoal(),
                localizedTemplate == null ? plan.getLanguage() : localizedTemplate.defaultLanguage(),
                plan.getLevel(),
                localizedTemplate == null ? plan.getDuration() : localizedTemplate.duration(),
                toLearningStatus(plan.getStatus()),
                plan.getProgress(),
                lessonService.findByPlanId(plan.getId()).stream()
                        .map(lesson -> lessonService.toSummaryResponse(lesson, language))
                        .toList());
    }

    private LearningStatus toLearningStatus(String value) {
        return value == null ? LearningStatus.NOT_STARTED : LearningStatus.fromValue(value);
    }
}
