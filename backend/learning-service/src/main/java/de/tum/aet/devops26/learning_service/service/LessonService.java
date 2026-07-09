package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.GenerateAiExercisesRequest;
import de.tum.aet.devops26.learning_service.dto.GenerateAiExercisesResponse;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.dto.LessonSummaryResponse;
import de.tum.aet.devops26.learning_service.dto.LessonResponse;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.model.LessonContentBlock;
import de.tum.aet.devops26.learning_service.repository.LessonContentBlockRepository;
import de.tum.aet.devops26.learning_service.repository.LessonRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ExerciseService exerciseService;
    private final LessonContentBlockRepository lessonContentBlockRepository;

    public Lesson save(Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    public List<LessonContentBlock> saveContentBlocks(Long lessonId, List<String> contentBlocks) {
        if (contentBlocks == null || contentBlocks.isEmpty()) {
            return List.of();
        }

        List<String> normalizedBlocks = contentBlocks.stream()
            .map(contentBlock -> contentBlock == null ? "" : contentBlock.trim())
            .filter(text -> !text.isBlank())
            .toList();
        int existingBlockCount = lessonContentBlockRepository.findByLessonIdOrderByOrderNumberAsc(lessonId).size();

        return IntStream.range(0, normalizedBlocks.size())
            .mapToObj(index -> lessonContentBlockRepository.save(LessonContentBlock.builder()
                .lessonId(lessonId)
                .orderNumber(existingBlockCount + index + 1)
                .type("content")
                .title("Lesson content")
                .text(normalizedBlocks.get(index))
                .build()))
            .toList();
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
        return findById(id).map(this::toResponse);
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
        List<?> exercises = exerciseService.findByLessonId(lesson.getId());
        return new LessonSummaryResponse(
            lesson.getId(),
            lesson.getPlanId(),
            lesson.getTitle(),
            lesson.getTopic(),
            lesson.getOrderNumber(),
            LearningStatus.NOT_STARTED,
            0,
            10,
            exercises.size()
        );
    }

    public LessonResponse toResponse(Lesson lesson) {
        List<de.tum.aet.devops26.learning_service.dto.ExerciseResponse> exercises = exerciseService.findByLessonId(lesson.getId()).stream()
            .map(exerciseService::toResponse)
            .toList();
        List<de.tum.aet.devops26.learning_service.dto.LessonContentBlock> contentBlocks = lessonContentBlockRepository
            .findByLessonIdOrderByOrderNumberAsc(lesson.getId()).stream()
            .map(this::toContentBlockResponse)
            .toList();

        return new LessonResponse(
            lesson.getId(),
            lesson.getPlanId(),
            lesson.getTitle(),
            lesson.getTopic(),
            lesson.getOrderNumber(),
            LearningStatus.NOT_STARTED,
            0,
            10,
            exercises.size(),
            contentBlocks,
            exercises
        );
    }

    private de.tum.aet.devops26.learning_service.dto.LessonContentBlock toContentBlockResponse(
        LessonContentBlock contentBlock
    ) {
        return new de.tum.aet.devops26.learning_service.dto.LessonContentBlock(
            de.tum.aet.devops26.learning_service.dto.LessonContentBlock.TypeEnum.fromValue(contentBlock.getType())
        )
            .title(contentBlock.getTitle())
            .subtitle(contentBlock.getSubtitle())
            .text(contentBlock.getText())
            .points(List.of());
    }
}
