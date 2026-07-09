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
import java.util.Comparator;
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
        List<ValidatedRagLesson> lessons = validateGeneratedPlan(generatedPlan);

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
        if (request.getMinimumLessons() == null || request.getMaximumLessons() == null
            || request.getMinimumLessons() > request.getMaximumLessons()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "minimum_lessons must be less than or equal to maximum_lessons"
            );
        }
    }

    private List<ValidatedRagLesson> validateGeneratedPlan(RagLearningPlanResponse generatedPlan) {
        if (generatedPlan == null || generatedPlan.lessons() == null || generatedPlan.lessons().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GenAI returned an empty RAG learning plan");
        }

        return generatedPlan.lessons().stream()
            .map(this::validateLesson)
            .sorted(Comparator.comparing(ValidatedRagLesson::orderNumber))
            .toList();
    }

    private ValidatedRagLesson validateLesson(RagLesson lesson) {
        if (lesson == null || lesson.orderNumber() == null || lesson.exercises() == null || lesson.exercises().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GenAI returned a malformed lesson");
        }

        return new ValidatedRagLesson(
            requiredText(lesson.title(), "lesson title"),
            requiredText(lesson.topic(), "lesson topic") + summarySuffix(lesson.summary()),
            lesson.orderNumber(),
            lesson.exercises().stream().map(this::validateExercise).toList()
        );
    }

    private ValidatedRagExercise validateExercise(RagExercise exercise) {
        if (exercise == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GenAI returned a malformed exercise");
        }

        ExerciseType type = parseExerciseType(exercise.type());
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

    private String summarySuffix(String summary) {
        return summary == null || summary.isBlank() ? "" : " - " + summary;
    }

    private record ValidatedRagLesson(
        String title,
        String topic,
        Integer orderNumber,
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
