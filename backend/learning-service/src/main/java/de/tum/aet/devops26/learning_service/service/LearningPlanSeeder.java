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
    static final String LISTENING_TITLE = "Software Engineering Interview Listening Practice";
    static final String SPEAKING_TITLE = "Software Engineering Interview Speaking Practice";
    static final String LEGACY_LISTENING_TITLE = "Everyday Listening Practice";
    static final String LEGACY_SPEAKING_TITLE = "Everyday Speaking Practice";

    private static final String LISTENING_DESCRIPTION =
        "Listening comprehension exercises based on software engineering interview scenarios.";
    private static final String LISTENING_DURATION = "1 week";
    private static final String LISTENING_GOAL =
        "Improve listening comprehension for software engineering interviews";
    private static final String LISTENING_LANGUAGE = "German";
    private static final String LISTENING_LEVEL = "A2";
    private static final String LISTENING_EXPECTED_ANSWER = "Select the most accurate listening response.";

    private static final String SPEAKING_DESCRIPTION =
        "Speaking exercises for software engineering interview responses.";
    private static final String SPEAKING_DURATION = "1 week";
    private static final String SPEAKING_GOAL =
        "Improve spoken answers for software engineering interviews";
    private static final String SPEAKING_LANGUAGE = "German";
    private static final String SPEAKING_LEVEL = "A2";

    private static final List<FixedLesson> FIXED_LISTENING_LESSONS = List.of(
        new FixedLesson(
            "Interview Self Introduction",
            "Listen to a candidate introduce their background, studies, and software engineering goals.",
            List.of(new FixedExercise(
                "AI listening exercise 1: software engineering interview self-introduction",
                LISTENING_EXPECTED_ANSWER
            ))
        ),
        new FixedLesson(
            "Project Deep Dive",
            "Listen to a candidate explain a software project, their role, and the technical result.",
            List.of(new FixedExercise(
                "AI listening exercise 1: software project interview explanation",
                LISTENING_EXPECTED_ANSWER
            ))
        ),
        new FixedLesson(
            "Debugging Discussion",
            "Listen to an interview answer about finding a bug, identifying the root cause, and verifying the fix.",
            List.of(new FixedExercise(
                "AI listening exercise 1: debugging interview answer",
                LISTENING_EXPECTED_ANSWER
            ))
        ),
        new FixedLesson(
            "System Design Conversation",
            "Listen to a simple system design explanation with requirements, API, database, and tradeoffs.",
            List.of(new FixedExercise(
                "AI listening exercise 1: software system design interview",
                LISTENING_EXPECTED_ANSWER
            ))
        ),
        new FixedLesson(
            "API and Database Interview",
            "Listen to an interview answer about API design, database choice, and data consistency.",
            List.of(new FixedExercise(
                "AI listening exercise 1: API and database interview tradeoffs",
                LISTENING_EXPECTED_ANSWER
            ))
        ),
        new FixedLesson(
            "Testing and Code Review",
            "Listen to a candidate discuss test strategy, code review, and maintainability.",
            List.of(new FixedExercise(
                "AI listening exercise 1: testing and code review interview",
                LISTENING_EXPECTED_ANSWER
            ))
        ),
        new FixedLesson(
            "Team Collaboration",
            "Listen to an answer about resolving a technical disagreement with a teammate.",
            List.of(new FixedExercise(
                "AI listening exercise 1: software team collaboration interview",
                LISTENING_EXPECTED_ANSWER
            ))
        ),
        new FixedLesson(
            "Prioritization and Ownership",
            "Listen to a candidate explain how they prioritize engineering work and own mistakes.",
            List.of(new FixedExercise(
                "AI listening exercise 1: engineering prioritization and ownership interview",
                LISTENING_EXPECTED_ANSWER
            ))
        )
    );

    private static final List<FixedLesson> FIXED_SPEAKING_LESSONS = List.of(
        new FixedLesson(
            "Self Introduction",
            "Giving a short spoken software engineering interview introduction.",
            List.of(new FixedExercise(
                "Record a 45-second answer to: Tell me about yourself as a software engineering candidate.",
                "Mention your background, studies or experience, technical strengths, and one professional goal."
            ))
        ),
        new FixedLesson(
            "Project Deep Dive",
            "Explaining a software project clearly out loud.",
            List.of(new FixedExercise(
                "Record how you would answer: Describe a software project you are proud of.",
                "Describe the problem, your role, important technical decisions, and the result."
            ))
        ),
        new FixedLesson(
            "Debugging Story",
            "Explaining debugging and problem-solving in an interview.",
            List.of(new FixedExercise(
                "Record how you would answer: Tell me about a difficult bug you fixed.",
                "Explain the symptom, investigation, root cause, fix, and how you verified the solution."
            ))
        ),
        new FixedLesson(
            "System Design Tradeoffs",
            "Speaking through architecture decisions and tradeoffs.",
            List.of(new FixedExercise(
                "Record how you would explain the difference between monoliths and microservices.",
                "Compare coupling, deployment, team ownership, complexity, and tradeoffs."
            ))
        ),
        new FixedLesson(
            "API Design",
            "Speaking about developer-friendly APIs.",
            List.of(new FixedExercise(
                "Record how you would answer: What makes a REST API easy for other developers to use?",
                "Mention resource naming, status codes, validation, error responses, and documentation."
            ))
        ),
        new FixedLesson(
            "Database Choice",
            "Explaining persistence tradeoffs out loud.",
            List.of(new FixedExercise(
                "Record how you would answer: When would you choose a relational database instead of a document database?",
                "Compare schema, relationships, transactions, query patterns, and consistency needs."
            ))
        ),
        new FixedLesson(
            "Testing and Review",
            "Speaking about quality practices in engineering teams.",
            List.of(new FixedExercise(
                "Record how you would answer: What do you look for when reviewing another engineer's pull request?",
                "Mention correctness, readability, maintainability, tests, and security."
            ))
        ),
        new FixedLesson(
            "Prioritization",
            "Explaining engineering judgment under constraints.",
            List.of(new FixedExercise(
                "Record how you would answer: How do you prioritize engineering tasks when everything feels important?",
                "Mention user impact, urgency, dependencies, risk, and communication."
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
                    .type(valueOrDefault(exerciseTemplate.subtype(), ExerciseSubtype.FREE_TEXT.getValue()))
                    .question(exerciseTemplate.question())
                    .difficulty(plan.getLevel())
                    .expectedAnswer(valueOrDefault(exerciseTemplate.expectedAnswer(), template.defaultExpectedAnswer()))
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
