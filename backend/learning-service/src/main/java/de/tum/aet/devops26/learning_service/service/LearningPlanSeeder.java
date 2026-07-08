package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.ExerciseSubtype;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the creation of fixed learning plans in isolated REQUIRES_NEW transactions so that
 * a DataIntegrityViolationException from a concurrent insert on the (user_id, title) unique
 * constraint rolls back only the inner transaction and does not taint the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class LearningPlanSeeder {

    private static final String DEFAULT_TEMPLATE_KEY = "job-interview";

    static final String DEFAULT_TITLE = "Job Interview Preparation";
    static final String LISTENING_TITLE = "Everyday Listening Practice";

    private static final String LISTENING_DESCRIPTION = "Listening comprehension exercises on everyday German topics.";
    private static final String LISTENING_DURATION = "1 week";
    private static final String LISTENING_GOAL = "Improve German listening comprehension";
    private static final String LISTENING_LANGUAGE = "German";
    private static final String LISTENING_LEVEL = "A2";
    private static final String LISTENING_EXPECTED_ANSWER = "Select the most accurate listening response.";

    private static final List<FixedLesson> FIXED_LISTENING_LESSONS = List.of(
        new FixedLesson(
            "At the Café",
            "Ordering drinks and food at a German café.",
            List.of("AI listening exercise 1: café conversation")
        ),
        new FixedLesson(
            "Public Transport",
            "Asking for directions and using public transport in Germany.",
            List.of("AI listening exercise 1: public transport")
        ),
        new FixedLesson(
            "Shopping",
            "Buying items and asking about prices in a German shop.",
            List.of("AI listening exercise 1: shopping")
        )
    );

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;
    private final DefaultLearningPlanCatalog defaultLearningPlanCatalog;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LearningPlan createDefaultPlan(CreateDefaultLearningPlanRequest request) {
        DefaultLearningPlanContent template = defaultLearningPlanCatalog.findFallbackByKey(DEFAULT_TEMPLATE_KEY);

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

        for (int i = 0; i < template.lessons().size(); i++) {
            DefaultLessonTemplate lessonTemplate = template.lessons().get(i);
            Lesson lesson = lessonService.save(Lesson.builder()
                .planId(plan.getId())
                .title(lessonTemplate.title())
                .topic(lessonTemplate.topic())
                .orderNumber(i + 1)
                .build());

            for (var exerciseTemplate : lessonTemplate.exercises()) {
                exerciseService.save(Exercise.builder()
                    .lessonId(lesson.getId())
                    .type("free_text")
                    .question(exerciseTemplate.question())
                    .difficulty(plan.getLevel())
                    .expectedAnswer(template.defaultExpectedAnswer())
                    .build());
            }
        }
        return plan;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createListeningPlan(CreateDefaultLearningPlanRequest request) {
        LearningPlan plan = learningPlanRepository.save(LearningPlan.builder()
            .userId(request.getUserId())
            .title(LISTENING_TITLE)
            .description(LISTENING_DESCRIPTION)
            .goal(LISTENING_GOAL)
            .language(valueOrDefault(request.getTargetLanguage(), LISTENING_LANGUAGE))
            .level(valueOrDefault(request.getCurrentLevel(), LISTENING_LEVEL))
            .duration(LISTENING_DURATION)
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build());

        for (int i = 0; i < FIXED_LISTENING_LESSONS.size(); i++) {
            FixedLesson fl = FIXED_LISTENING_LESSONS.get(i);
            Lesson lesson = lessonService.save(Lesson.builder()
                .planId(plan.getId())
                .title(fl.title())
                .topic(fl.topic())
                .orderNumber(i + 1)
                .build());

            for (String question : fl.exercises()) {
                exerciseService.save(Exercise.builder()
                    .lessonId(lesson.getId())
                    .type(ExerciseSubtype.LISTENING_CHOICE.getValue())
                    .question(question)
                    .difficulty(plan.getLevel())
                    .expectedAnswer(LISTENING_EXPECTED_ANSWER)
                    .build());
            }
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    record FixedLesson(String title, String topic, List<String> exercises) {}
}
