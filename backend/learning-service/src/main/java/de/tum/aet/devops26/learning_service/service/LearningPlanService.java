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
    private static final String DEFAULT_DESCRIPTION = "Fixed lessons for practicing professional job interview answers.";
    private static final String DEFAULT_DURATION = "2 weeks";
    private static final String DEFAULT_GOAL = "Prepare for a professional job interview";
    private static final String DEFAULT_LANGUAGE = "English";
    private static final String DEFAULT_LEVEL = "A2";
    private static final String DEFAULT_EXPECTED_ANSWER = "Write a clear, professional answer using specific details and formal vocabulary.";
    private static final List<FixedLesson> FIXED_INTERVIEW_LESSONS = List.of(
        new FixedLesson(
            "Self Introduction",
            "Introduce yourself professionally in an interview.",
            List.of(
                "Tell me about yourself.",
                "Write a short professional introduction.",
                "Improve your introduction using more formal vocabulary."
            )
        ),
        new FixedLesson(
            "Education and Background",
            "Explain your studies, university, and academic background.",
            List.of(
                "Describe your degree and specialization.",
                "Explain why you chose your field.",
                "Practice saying your graduation status clearly."
            )
        ),
        new FixedLesson(
            "Work Experience and Internships",
            "Talk about previous internships, jobs, or projects.",
            List.of(
                "Describe one internship or work experience.",
                "Explain your responsibilities.",
                "Mention what you learned from the experience."
            )
        ),
        new FixedLesson(
            "Project Explanation",
            "Present a technical or academic project clearly.",
            List.of(
                "Describe one project you worked on.",
                "Explain the problem, your solution, and your role.",
                "Simplify a technical explanation for a non-technical interviewer."
            )
        ),
        new FixedLesson(
            "Strengths and Weaknesses",
            "Answer common HR questions about strengths and weaknesses.",
            List.of(
                "Name two strengths with examples.",
                "Explain one weakness professionally.",
                "Rewrite weak answers into stronger interview answers."
            )
        )
    );

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;

    @Transactional
    public LearningPlanResponse createDefaultLearningPlan(CreateDefaultLearningPlanRequest request) {
        return learningPlanRepository.findFirstByUserIdAndTitle(request.getUserId(), DEFAULT_TITLE)
            .map(this::toResponse)
            .orElseGet(() -> createFixedDefaultLearningPlan(request));
    }

    private LearningPlanResponse createFixedDefaultLearningPlan(CreateDefaultLearningPlanRequest request) {
        LearningPlan plan = learningPlanRepository.save(LearningPlan.builder()
            .userId(request.getUserId())
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .goal(valueOrDefault(request.getLearningGoal(), DEFAULT_GOAL))
            .language(valueOrDefault(request.getTargetLanguage(), DEFAULT_LANGUAGE))
            .level(valueOrDefault(request.getCurrentLevel(), DEFAULT_LEVEL))
            .duration(DEFAULT_DURATION)
            .status(LearningStatus.NOT_STARTED.getValue())
            .progress(0)
            .build());

        for (int lessonIndex = 0; lessonIndex < FIXED_INTERVIEW_LESSONS.size(); lessonIndex++) {
            FixedLesson fixedLesson = FIXED_INTERVIEW_LESSONS.get(lessonIndex);
            Lesson lesson = lessonService.save(Lesson.builder()
                .planId(plan.getId())
                .title(fixedLesson.title())
                .topic(fixedLesson.topic())
                .orderNumber(lessonIndex + 1)
                .build());

            for (String question : fixedLesson.exercises()) {
                exerciseService.save(Exercise.builder()
                    .lessonId(lesson.getId())
                    .type("free_text")
                    .question(question)
                    .difficulty(plan.getLevel())
                    .expectedAnswer(DEFAULT_EXPECTED_ANSWER)
                    .build());
            }
        }

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

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record FixedLesson(String title, String topic, List<String> exercises) {
    }
}
