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
 * Handles fixed learning plan creation in isolated transactions so concurrent
 * inserts on the (user_id, title) unique constraint do not taint the caller.
 */
@Service
@RequiredArgsConstructor
public class LearningPlanSeeder {

    private static final String DEFAULT_TEMPLATE_KEY = "job-interview";

    static final String DEFAULT_TITLE = "Job Interview Preparation";
    static final String LISTENING_TITLE = "Everyday Listening Practice";
    static final String SPEAKING_TITLE = "Everyday Speaking Practice";

    private static final String LISTENING_DESCRIPTION = "Listening comprehension exercises on everyday German topics.";
    private static final String LISTENING_DURATION = "1 week";
    private static final String LISTENING_GOAL = "Improve German listening comprehension";
    private static final String LISTENING_LANGUAGE = "German";
    private static final String LISTENING_LEVEL = "A2";
    private static final String LISTENING_EXPECTED_ANSWER = "Select the most accurate listening response.";

    private static final String SPEAKING_DESCRIPTION =
        "Speaking exercises for short German responses in everyday and interview situations.";
    private static final String SPEAKING_DURATION = "1 week";
    private static final String SPEAKING_GOAL = "Improve German spoken responses";
    private static final String SPEAKING_LANGUAGE = "German";
    private static final String SPEAKING_LEVEL = "A2";

    private static final List<FixedLesson> FIXED_LISTENING_LESSONS = List.of(
        new FixedLesson(
            "At the Cafe",
            "Ordering drinks and food at a German cafe.",
            List.of(new FixedExercise("AI listening exercise 1: cafe conversation", LISTENING_EXPECTED_ANSWER))
        ),
        new FixedLesson(
            "Public Transport",
            "Asking for directions and using public transport in Germany.",
            List.of(new FixedExercise("AI listening exercise 1: public transport", LISTENING_EXPECTED_ANSWER))
        ),
        new FixedLesson(
            "Shopping",
            "Buying items and asking about prices in a German shop.",
            List.of(new FixedExercise("AI listening exercise 1: shopping", LISTENING_EXPECTED_ANSWER))
        )
    );

    private static final List<FixedLesson> FIXED_SPEAKING_LESSONS = List.of(
        new FixedLesson(
            "Self Introduction",
            "Giving a short spoken introduction in German.",
            List.of(new FixedExercise(
                "Record a 30-second introduction with your name, studies, and one professional goal.",
                "Mention your name, studies, and one professional goal in a concise spoken response."
            ))
        ),
        new FixedLesson(
            "At the Cafe",
            "Ordering politely in German.",
            List.of(new FixedExercise(
                "Record how you would order a coffee and ask for the price.",
                "Politely order a coffee and ask how much it costs."
            ))
        ),
        new FixedLesson(
            "Project Pitch",
            "Explaining a project out loud.",
            List.of(new FixedExercise(
                "Record a short explanation of a project you worked on and your role.",
                "Describe the project, the problem it solved, and your personal role."
            ))
        )
    );

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;
    private final DefaultLearningPlanCatalog defaultLearningPlanCatalog;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LearningPlan createDefaultPlan(CreateDefaultLearningPlanRequest request) {
        return createDefaultPlan(request, DEFAULT_TEMPLATE_KEY);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LearningPlan createDefaultPlan(CreateDefaultLearningPlanRequest request, String templateKey) {
        DefaultLearningPlanContent template = defaultLearningPlanCatalog.findFallbackByKey(templateKey);

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
    public LearningPlan createListeningPlan(CreateDefaultLearningPlanRequest request) {
        LearningPlan plan = createPlan(
            request,
            LISTENING_TITLE,
            LISTENING_DESCRIPTION,
            LISTENING_GOAL,
            valueOrDefault(request.getTargetLanguage(), LISTENING_LANGUAGE),
            valueOrDefault(request.getCurrentLevel(), LISTENING_LEVEL),
            LISTENING_DURATION
        );
        createLessons(plan, FIXED_LISTENING_LESSONS, ExerciseSubtype.LISTENING_CHOICE);
        return plan;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LearningPlan createSpeakingPlan(CreateDefaultLearningPlanRequest request) {
        LearningPlan plan = createPlan(
            request,
            SPEAKING_TITLE,
            SPEAKING_DESCRIPTION,
            SPEAKING_GOAL,
            valueOrDefault(request.getTargetLanguage(), SPEAKING_LANGUAGE),
            valueOrDefault(request.getCurrentLevel(), SPEAKING_LEVEL),
            SPEAKING_DURATION
        );
        createLessons(plan, FIXED_SPEAKING_LESSONS, ExerciseSubtype.SPEAKING_PROMPT);
        return plan;
    }

    private LearningPlan createPlan(
        CreateDefaultLearningPlanRequest request,
        String title,
        String description,
        String goal,
        String language,
        String level,
        String duration
    ) {
        return learningPlanRepository.save(LearningPlan.builder()
            .userId(request.getUserId())
            .title(title)
            .description(description)
            .goal(goal)
            .language(language)
            .level(level)
            .duration(duration)
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build());
    }

    private void createLessons(LearningPlan plan, List<FixedLesson> fixedLessons, ExerciseSubtype subtype) {
        for (int lessonIndex = 0; lessonIndex < fixedLessons.size(); lessonIndex++) {
            FixedLesson fixedLesson = fixedLessons.get(lessonIndex);
            Lesson lesson = lessonService.save(Lesson.builder()
                .planId(plan.getId())
                .title(fixedLesson.title())
                .topic(fixedLesson.topic())
                .orderNumber(lessonIndex + 1)
                .build());

            for (FixedExercise fixedExercise : fixedLesson.exercises()) {
                exerciseService.save(Exercise.builder()
                    .lessonId(lesson.getId())
                    .type(subtype.getValue())
                    .question(fixedExercise.question())
                    .difficulty(plan.getLevel())
                    .expectedAnswer(fixedExercise.expectedAnswer())
                    .build());
            }
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record FixedLesson(String title, String topic, List<FixedExercise> exercises) {
    }

    private record FixedExercise(String question, String expectedAnswer) {
    }
}
