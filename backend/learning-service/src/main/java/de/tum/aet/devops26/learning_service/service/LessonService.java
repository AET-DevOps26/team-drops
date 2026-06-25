package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.GenerateAiExercisesRequest;
import de.tum.aet.devops26.learning_service.dto.GenerateAiExercisesResponse;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.dto.LessonSummaryResponse;
import de.tum.aet.devops26.learning_service.dto.LessonResponse;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import de.tum.aet.devops26.learning_service.repository.LessonRepository;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanCatalog;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanContent;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLessonTemplate;
import de.tum.aet.devops26.learning_service.service.catalog.LocalizedExercise;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonService {

    private static final String DEFAULT_TEMPLATE_KEY = "job-interview";

    private final LessonRepository lessonRepository;
    private final ExerciseService exerciseService;
    private final LearningPlanRepository learningPlanRepository;
    private final DefaultLearningPlanCatalog defaultLearningPlanCatalog;

    public Lesson save(Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    public List<Lesson> findAll() {
        return lessonRepository.findAll();
    }

    public Optional<Lesson> findById(Long id) {
        return lessonRepository.findById(id);
    }

    public List<Lesson> findByPlanId(Long planId) {
        return lessonRepository.findByPlanIdOrderByOrderNumberAsc(planId);
    }

    public void deleteById(Long id) {
        lessonRepository.deleteById(id);
    }

    public Optional<LessonResponse> findResponseById(Long id) {
        return findResponseById(id, null);
    }

    public Optional<LessonResponse> findResponseById(Long id, String language) {
        return findById(id).map(lesson -> toResponse(lesson, language));
    }

    public Optional<GenerateAiExercisesResponse> generateAiExercisesForLesson(
        Long lessonId,
        GenerateAiExercisesRequest request
    ) {
        return findById(lessonId).map(lesson -> {
            List<Exercise> generatedExercises = IntStream.range(0, request.getCount())
                .mapToObj(index -> {
                    var exerciseType = request.getExerciseTypes().get(index % request.getExerciseTypes().size());
                    return exerciseService.save(Exercise.builder()
                        .lessonId(lessonId)
                        .type(exerciseService.defaultSubtypeFor(exerciseType).getValue())
                        .question(exerciseService.buildAiQuestion(lesson, exerciseType, request.getInstructions(), index + 1))
                        .difficulty("A2")
                        .expectedAnswer(exerciseService.defaultExpectedAnswer(exerciseType))
                        .build());
                })
                .toList();

            return new GenerateAiExercisesResponse(
                lessonId,
                generatedExercises.size(),
                generatedExercises.stream().map(exerciseService::toResponse).toList()
            );
        });
    }

    public LessonSummaryResponse toSummaryResponse(Lesson lesson) {
        return toSummaryResponse(lesson, null);
    }

    public LessonSummaryResponse toSummaryResponse(Lesson lesson, String language) {
        DefaultLessonTemplate localizedLesson = localizedLessonFor(lesson, language).orElse(null);
        List<?> exercises = exerciseService.findByLessonId(lesson.getId());
        return new LessonSummaryResponse(
            lesson.getId(),
            lesson.getPlanId(),
            localizedLesson == null ? lesson.getTitle() : localizedLesson.title(),
            localizedLesson == null ? lesson.getTopic() : localizedLesson.topic(),
            lesson.getOrderNumber(),
            LearningStatus.NOT_STARTED,
            0,
            10,
            exercises.size()
        );
    }

    public LessonResponse toResponse(Lesson lesson) {
        return toResponse(lesson, null);
    }

    public LessonResponse toResponse(Lesson lesson, String language) {
        DefaultLessonTemplate localizedLesson = localizedLessonFor(lesson, language).orElse(null);
        List<de.tum.aet.devops26.learning_service.dto.ExerciseResponse> exercises = exerciseService.findByLessonId(lesson.getId()).stream()
            .map(exercise -> exerciseService.toResponse(exercise, localizedExerciseFor(exercise, localizedLesson, language)))
            .toList();

        return new LessonResponse(
            lesson.getId(),
            lesson.getPlanId(),
            localizedLesson == null ? lesson.getTitle() : localizedLesson.title(),
            localizedLesson == null ? lesson.getTopic() : localizedLesson.topic(),
            lesson.getOrderNumber(),
            LearningStatus.NOT_STARTED,
            0,
            10,
            exercises.size(),
            List.of(),
            exercises
        );
    }

    private Optional<DefaultLessonTemplate> localizedLessonFor(Lesson lesson, String language) {
        return learningPlanRepository.findById(lesson.getPlanId())
            .filter(this::isDefaultLearningPlan)
            .map(plan -> defaultLearningPlanCatalog.findLocalizedByKey(DEFAULT_TEMPLATE_KEY, language))
            .filter(content -> lesson.getOrderNumber() != null
                && lesson.getOrderNumber() > 0
                && lesson.getOrderNumber() <= content.lessons().size())
            .map(content -> content.lessons().get(lesson.getOrderNumber() - 1));
    }

    private LocalizedExercise localizedExerciseFor(Exercise exercise, DefaultLessonTemplate localizedLesson, String language) {
        if (localizedLesson == null) {
            return null;
        }

        List<Exercise> lessonExercises = exerciseService.findByLessonId(exercise.getLessonId());
        int exerciseIndex = IntStream.range(0, lessonExercises.size())
            .filter(index -> exercise.getId().equals(lessonExercises.get(index).getId()))
            .findFirst()
            .orElse(-1);

        if (exerciseIndex < 0 || exerciseIndex >= localizedLesson.exercises().size()) {
            return null;
        }

        DefaultLearningPlanContent content = defaultLearningPlanCatalog.findLocalizedByKey(DEFAULT_TEMPLATE_KEY, language);
        return new LocalizedExercise(
            localizedLesson.exercises().get(exerciseIndex),
            content.defaultExpectedAnswer(),
            localizedFormatFor(language)
        );
    }

    private boolean isDefaultLearningPlan(LearningPlan plan) {
        return defaultLearningPlanCatalog.hasLocalizedTitle(DEFAULT_TEMPLATE_KEY, plan.getTitle());
    }

    private String localizedFormatFor(String language) {
        return "German".equalsIgnoreCase(language == null ? "" : language.trim())
            ? "Kurze schriftliche Antwort"
            : null;
    }
}
