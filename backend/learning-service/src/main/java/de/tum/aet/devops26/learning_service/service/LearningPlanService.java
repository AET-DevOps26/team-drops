package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.CreateAiLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.ExerciseSubtype;
import de.tum.aet.devops26.learning_service.dto.ExerciseType;
import de.tum.aet.devops26.learning_service.dto.LearningPlanResponse;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient.RagExercise;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient.RagLearningPlanResponse;
import de.tum.aet.devops26.learning_service.integration.GenAiRagLearningPlanClient.RagLesson;
import de.tum.aet.devops26.learning_service.integration.UserServiceClient;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
    private static final int MIN_DURATION_WEEKS = 1;
    private static final int MAX_DURATION_WEEKS = 52;
    private static final int MIN_STUDY_HOURS_PER_WEEK = 1;
    private static final int MAX_STUDY_HOURS_PER_WEEK = 80;
    private static final int MIN_LESSONS = 1;
    private static final int MAX_LESSONS = 24;

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;
    private final LearningPlanSeeder learningPlanSeeder;
    private final GenAiRagLearningPlanClient genAiRagLearningPlanClient;
    private final UserServiceClient userServiceClient;

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
        validateAiLearningPlanRequest(request);
        Long resolvedUserId = userServiceClient.resolveSubmittedUserId(request.getUserId());
        RagLearningPlanResponse generatedPlan = genAiRagLearningPlanClient.generate(request);
        List<ValidatedRagLesson> lessons = validateGeneratedPlan(generatedPlan, request);

        LearningPlan plan = learningPlanRepository.save(LearningPlan.builder()
            .userId(resolvedUserId)
            .title(requiredText(generatedPlan.title(), "title"))
            .description(requiredText(generatedPlan.description(), "description"))
            .goal(requiredText(generatedPlan.goal(), "goal"))
            .language(requiredText(generatedPlan.language(), "language"))
            .level(requiredText(generatedPlan.level(), "level"))
            .duration(requiredText(generatedPlan.duration(), "duration"))
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build());

        for (ValidatedRagLesson generatedLesson : lessons) {
            Lesson lesson = lessonService.save(Lesson.builder()
                .planId(plan.getId())
                .title(generatedLesson.title())
                .topic(generatedLesson.topic())
                .orderNumber(generatedLesson.orderNumber())
                .build());
            lessonService.saveContentBlocks(lesson.getId(), generatedLesson.contentBlocks());

            for (ValidatedRagExercise generatedExercise : generatedLesson.exercises()) {
                exerciseService.save(Exercise.builder()
                    .lessonId(lesson.getId())
                    .type(generatedExercise.subtype().getValue())
                    .question(generatedExercise.question())
                    .difficulty(generatedExercise.difficulty())
                    .expectedAnswer(generatedExercise.expectedAnswer())
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

    private void validateAiLearningPlanRequest(CreateAiLearningPlanRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI learning-plan request must not be null");
        }
        if (request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id is required");
        }
        requireRequestText(request.getRagTopic(), "rag_topic");
        requireRequestText(request.getLearningGoal(), "learning_goal");
        requireRequestText(request.getTargetLanguage(), "target_language");
        requireRequestText(request.getCurrentLevel(), "current_level");
        requireBetween(request.getDurationWeeks(), MIN_DURATION_WEEKS, MAX_DURATION_WEEKS, "duration_weeks");
        requireBetween(
            request.getStudyHoursPerWeek(),
            MIN_STUDY_HOURS_PER_WEEK,
            MAX_STUDY_HOURS_PER_WEEK,
            "study_hours_per_week"
        );
        requireBetween(request.getMinimumLessons(), MIN_LESSONS, MAX_LESSONS, "minimum_lessons");
        requireBetween(request.getMaximumLessons(), MIN_LESSONS, MAX_LESSONS, "maximum_lessons");
        if (request.getExerciseTypes() == null || request.getExerciseTypes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "exercise_types must contain at least one value");
        }
        if (request.getMinimumLessons() == null || request.getMaximumLessons() == null
            || request.getMinimumLessons() > request.getMaximumLessons()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "minimum_lessons must be less than or equal to maximum_lessons"
            );
        }
    }

    private void requireRequestText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
    }

    private void requireBetween(Integer value, int minimum, int maximum, String field) {
        if (value == null || value < minimum || value > maximum) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                field + " must be between " + minimum + " and " + maximum
            );
        }
    }

    private List<ValidatedRagLesson> validateGeneratedPlan(
        RagLearningPlanResponse generatedPlan,
        CreateAiLearningPlanRequest request
    ) {
        if (generatedPlan == null || generatedPlan.lessons() == null || generatedPlan.lessons().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GenAI returned an empty RAG learning plan");
        }

        int lessonCount = generatedPlan.lessons().size();
        if (lessonCount < request.getMinimumLessons() || lessonCount > request.getMaximumLessons()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "GenAI returned lesson count outside requested bounds"
            );
        }

        Set<ExerciseType> requestedExerciseTypes = EnumSet.copyOf(request.getExerciseTypes());
        List<ValidatedRagLesson> lessons = generatedPlan.lessons().stream()
            .map(lesson -> validateLesson(lesson, requestedExerciseTypes))
            .sorted(Comparator.comparing(ValidatedRagLesson::orderNumber))
            .toList();
        validateContiguousLessonOrder(lessons);
        return lessons;
    }

    private void validateContiguousLessonOrder(List<ValidatedRagLesson> lessons) {
        for (int index = 0; index < lessons.size(); index++) {
            int expectedOrder = index + 1;
            if (lessons.get(index).orderNumber() != expectedOrder) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "GenAI returned duplicate, missing, or non-contiguous lesson order numbers"
                );
            }
        }
    }

    private ValidatedRagLesson validateLesson(RagLesson lesson, Set<ExerciseType> requestedExerciseTypes) {
        if (lesson == null || lesson.orderNumber() == null || lesson.exercises() == null || lesson.exercises().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GenAI returned a malformed lesson");
        }

        return new ValidatedRagLesson(
            requiredText(lesson.title(), "lesson title"),
            requiredText(lesson.topic(), "lesson topic"),
            lesson.orderNumber(),
            normalizedContentBlocks(lesson.summary(), lesson.contentBlocks()),
            lesson.exercises().stream()
                .map(exercise -> validateExercise(exercise, requestedExerciseTypes))
                .toList()
        );
    }

    private List<String> normalizedContentBlocks(String summary, List<String> contentBlocks) {
        List<String> normalized = new ArrayList<>();
        if (summary != null && !summary.isBlank()) {
            normalized.add(summary.trim());
        }
        if (contentBlocks != null) {
            contentBlocks.stream()
                .filter(contentBlock -> contentBlock != null && !contentBlock.isBlank())
                .map(String::trim)
                .filter(contentBlock -> normalized.isEmpty() || !contentBlock.equals(normalized.getLast()))
                .forEach(normalized::add);
        }
        return normalized;
    }

    private ValidatedRagExercise validateExercise(RagExercise exercise, Set<ExerciseType> requestedExerciseTypes) {
        if (exercise == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GenAI returned a malformed exercise");
        }

        ExerciseType type = parseExerciseType(exercise.type());
        if (!requestedExerciseTypes.contains(type)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "GenAI returned an exercise type that was not requested: " + exercise.type()
            );
        }

        ExerciseSubtype subtype = parseExerciseSubtype(exercise.subtype());
        if (typeForSubtype(subtype) != type) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "GenAI returned incompatible exercise type/subtype: "
                    + exercise.type() + "/" + exercise.subtype()
            );
        }

        return new ValidatedRagExercise(
            subtype,
            requiredText(exercise.question(), "exercise question"),
            requiredText(exercise.expectedAnswer(), "exercise expected_answer"),
            requiredText(exercise.difficulty(), "exercise difficulty")
        );
    }

    private ExerciseType parseExerciseType(String value) {
        try {
            return ExerciseType.fromValue(requiredText(value, "exercise type"));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "GenAI returned unsupported exercise type: " + value,
                exception
            );
        }
    }

    private ExerciseSubtype parseExerciseSubtype(String value) {
        try {
            return ExerciseSubtype.fromValue(requiredText(value, "exercise subtype"));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "GenAI returned unsupported exercise subtype: " + value,
                exception
            );
        }
    }

    private ExerciseType typeForSubtype(ExerciseSubtype subtype) {
        return switch (subtype) {
            case MULTIPLE_CHOICE -> ExerciseType.READING;
            case LISTENING_CHOICE -> ExerciseType.LISTENING;
            case SPEAKING_PROMPT -> ExerciseType.SPEAKING;
            case TRANSLATION, FILL_IN_BLANK, SENTENCE_BUILDING, FREE_TEXT -> ExerciseType.WRITING;
        };
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GenAI returned blank " + field);
        }
        return value;
    }

    private record ValidatedRagLesson(
        String title,
        String topic,
        Integer orderNumber,
        List<String> contentBlocks,
        List<ValidatedRagExercise> exercises
    ) {
    }

    private record ValidatedRagExercise(
        ExerciseSubtype subtype,
        String question,
        String expectedAnswer,
        String difficulty
    ) {
    }
}
