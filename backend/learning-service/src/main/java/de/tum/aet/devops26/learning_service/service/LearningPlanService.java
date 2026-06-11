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

    private static final String DEFAULT_TITLE = "Machine Learning Interview Track";
    private static final String DEFAULT_DESCRIPTION = "German interview practice for explaining machine learning projects, systems, and production work.";
    private static final String DEFAULT_DURATION = "4 weeks";
    private static final String DEFAULT_GOAL = "Prepare for machine learning interviews in German";
    private static final String DEFAULT_LANGUAGE = "German";
    private static final String DEFAULT_LEVEL = "B1";
    private static final List<FixedLesson> FIXED_INTERVIEW_LESSONS = List.of(
        new FixedLesson(
            "Presenting an ML Project",
            "Explain an ML project in German with the problem, dataset, model, result, and personal contribution.",
            List.of(
                new FixedExercise(
                    "Describe a machine learning project you worked on.",
                    "Answer in German and explain the problem, dataset, model, and result."
                ),
                new FixedExercise(
                    "What was your specific role in the project?",
                    "Answer in German in 3-5 sentences."
                ),
                new FixedExercise(
                    "What was the biggest technical challenge, and how did you solve it?",
                    "Answer in German with a clear challenge-solution-result structure."
                )
            )
        ),
        new FixedLesson(
            "Data Preparation",
            "Discuss how training data is cleaned, transformed, and made suitable for machine learning.",
            List.of(
                new FixedExercise(
                    "How did you prepare the data before training the model?",
                    "Use useful German vocabulary such as die Daten bereinigen, Merkmale auswahlen, die Daten normalisieren, and Ausreisser entfernen."
                ),
                new FixedExercise(
                    "How would you handle missing values in a dataset?",
                    "Answer in German and include the phrase fehlende Werte."
                ),
                new FixedExercise(
                    "What would you do if the dataset were imbalanced?",
                    "Answer in German and include the phrase unausgeglichener Datensatz."
                )
            )
        ),
        new FixedLesson(
            "Model Selection",
            "Compare models and justify choices using technical and practical tradeoffs.",
            List.of(
                new FixedExercise(
                    "Why did you choose this machine learning model?",
                    "Explain model complexity, interpretability, training time, dataset size, and performance."
                ),
                new FixedExercise(
                    "How would you compare two different models?",
                    "Answer in German and mention evaluation metrics plus practical constraints."
                ),
                new FixedExercise(
                    "When would you use a decision tree instead of a neural network?",
                    "Answer in German and contrast interpretability, data size, training time, and performance."
                )
            )
        ),
        new FixedLesson(
            "Training and Overfitting",
            "Explain training concepts, data splits, hyperparameters, and overfitting prevention.",
            List.of(
                new FixedExercise(
                    "What is overfitting, and how can it be prevented?",
                    "Use German vocabulary such as Uberanpassung, Regularisierung, and Kreuzvalidierung."
                ),
                new FixedExercise(
                    "What is the difference between training, validation, and test data?",
                    "Use the terms Trainingsdaten, Validierungsdaten, and Testdaten."
                ),
                new FixedExercise(
                    "How do hyperparameters affect model training?",
                    "Answer in German and explain how hyperparameters are selected or tuned."
                )
            )
        ),
        new FixedLesson(
            "Model Evaluation",
            "Evaluate ML models and explain metrics clearly in German.",
            List.of(
                new FixedExercise(
                    "Which metrics did you use to evaluate your model?",
                    "Cover relevant metrics such as Accuracy, Precision, Recall, F1-Score, confusion matrix, or ROC-AUC."
                ),
                new FixedExercise(
                    "What is the difference between precision and recall?",
                    "Answer in German and give a short practical example."
                ),
                new FixedExercise(
                    "Why can accuracy be misleading?",
                    "Answer in German and relate the explanation to imbalanced datasets."
                ),
                new FixedExercise(
                    "How would you interpret a confusion matrix?",
                    "Explain true positives, false positives, true negatives, and false negatives in German."
                )
            )
        ),
        new FixedLesson(
            "Improving Model Performance",
            "Diagnose weak model performance and improve it systematically.",
            List.of(
                new FixedExercise(
                    "What would you do if the model performed poorly?",
                    "Answer in German and follow this order: identify the problem, inspect the data, test a baseline, change one factor, evaluate again."
                ),
                new FixedExercise(
                    "How would you improve the quality of the training data?",
                    "Answer in German and discuss cleaning, labeling quality, coverage, and outliers."
                ),
                new FixedExercise(
                    "How would you tune the model's hyperparameters?",
                    "Answer in German and mention validation data, grid search, random search, or cross-validation."
                )
            )
        ),
        new FixedLesson(
            "Deployment and Production",
            "Describe how trained models are served, monitored, and maintained in production.",
            List.of(
                new FixedExercise(
                    "How would you deploy a trained machine learning model?",
                    "Answer in German and discuss Docker, cloud deployment, and inference."
                ),
                new FixedExercise(
                    "How would another application communicate with the model?",
                    "Answer in German and explain a REST API or similar service interface."
                ),
                new FixedExercise(
                    "How would you monitor the model after deployment?",
                    "Answer in German and mention latency, monitoring, retraining, and quality metrics."
                ),
                new FixedExercise(
                    "What is model drift?",
                    "Answer in German and explain why retraining may become necessary."
                )
            )
        ),
        new FixedLesson(
            "ML System Design",
            "Design practical ML systems from input data through monitoring.",
            List.of(
                new FixedExercise(
                    "Design a system that recommends meals to users.",
                    "Explain input data, preprocessing, model, API, database, deployment, and monitoring."
                ),
                new FixedExercise(
                    "How would you build a spam detection system?",
                    "Explain input data, preprocessing, model, API, database, deployment, and monitoring."
                ),
                new FixedExercise(
                    "How would you design a real-time image classification service?",
                    "Explain input data, preprocessing, model, API, database, deployment, and monitoring."
                )
            )
        ),
        new FixedLesson(
            "Explaining ML to Non-Technical People",
            "Translate ML concepts into clear German explanations for non-technical stakeholders.",
            List.of(
                new FixedExercise(
                    "Explain overfitting to a non-technical manager.",
                    "Answer in German without relying on technical jargon."
                ),
                new FixedExercise(
                    "Explain how a recommendation system works without using technical terms.",
                    "Answer in simple German and use an everyday analogy."
                ),
                new FixedExercise(
                    "Explain why a model can make incorrect predictions.",
                    "Answer in German and focus on data limits, uncertainty, and changing real-world behavior."
                )
            )
        ),
        new FixedLesson(
            "ML Behavioral Questions",
            "Practice behavioral interview answers for ML work, teamwork, and continuous learning.",
            List.of(
                new FixedExercise(
                    "Tell me about a time when your model did not work as expected.",
                    "Answer in German with situation, action, and result."
                ),
                new FixedExercise(
                    "Describe a disagreement with a teammate about a technical decision.",
                    "Answer in German and show how you communicated and reached a decision."
                ),
                new FixedExercise(
                    "Tell me about a time when you had to learn a new ML technology quickly.",
                    "Answer in German and explain your learning strategy."
                ),
                new FixedExercise(
                    "How do you stay updated with developments in machine learning?",
                    "Answer in German and mention concrete sources, habits, or projects."
                )
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

            for (FixedExercise fixedExercise : fixedLesson.exercises()) {
                exerciseService.save(Exercise.builder()
                    .lessonId(lesson.getId())
                    .type("free_text")
                    .question(fixedExercise.question())
                    .difficulty(plan.getLevel())
                    .expectedAnswer(fixedExercise.expectedAnswer())
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

    private record FixedLesson(String title, String topic, List<FixedExercise> exercises) {
    }

    private record FixedExercise(String question, String expectedAnswer) {
    }
}
