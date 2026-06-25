package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.ExerciseSubtype;
import de.tum.aet.devops26.learning_service.dto.ExerciseResponse;
import de.tum.aet.devops26.learning_service.dto.ExerciseType;
import de.tum.aet.devops26.learning_service.dto.LearningStatus;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.ExerciseRepository;
import de.tum.aet.devops26.learning_service.service.catalog.LocalizedExercise;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public Exercise save(Exercise exercise) {
        return exerciseRepository.save(exercise);
    }

    public List<Exercise> findAll() {
        return exerciseRepository.findAll();
    }

    public Optional<Exercise> findById(Long id) {
        return exerciseRepository.findById(id);
    }

    public List<Exercise> findByLessonId(Long lessonId) {
        return exerciseRepository.findByLessonIdOrderByIdAsc(lessonId);
    }

    public void deleteById(Long id) {
        exerciseRepository.deleteById(id);
    }

    public ExerciseResponse toResponse(Exercise exercise) {
        return toResponse(exercise, null);
    }

    public ExerciseResponse toResponse(Exercise exercise, LocalizedExercise localizedExercise) {
        ExerciseSubtype subtype = toExerciseSubtype(exercise.getType());
        ExerciseType type = inferExerciseType(subtype);
        String question = localizedExercise == null ? exercise.getQuestion() : localizedExercise.question();
        String expectedAnswer = localizedExercise == null ? exercise.getExpectedAnswer() : localizedExercise.expectedAnswer();
        ExerciseResponse response = new ExerciseResponse(
            exercise.getId(),
            exercise.getLessonId(),
            type,
            subtype,
            question,
            question,
            exercise.getDifficulty(),
            expectedAnswer,
            LearningStatus.NOT_STARTED,
            buildFormat(type)
        );
        response.setSource(isAiExercise(exercise)
            ? ExerciseResponse.SourceEnum.AI
            : ExerciseResponse.SourceEnum.DEFAULT);
        if (localizedExercise != null && localizedExercise.format() != null) {
            response.setFormat(localizedExercise.format());
        }
        return response;
    }

    public String buildAiQuestion(Lesson lesson, ExerciseType exerciseType, String instructions, int itemNumber) {
        String suffix = (instructions == null || instructions.isBlank()) ? lesson.getTopic() : instructions;
        return "AI " + exerciseType.getValue() + " exercise " + itemNumber + ": " + suffix;
    }

    public ExerciseSubtype defaultSubtypeFor(ExerciseType exerciseType) {
        return switch (exerciseType) {
            case READING -> ExerciseSubtype.MULTIPLE_CHOICE;
            case LISTENING -> ExerciseSubtype.LISTENING_CHOICE;
            case SPEAKING -> ExerciseSubtype.SPEAKING_PROMPT;
            case WRITING -> ExerciseSubtype.FREE_TEXT;
        };
    }

    public String defaultExpectedAnswer(ExerciseType exerciseType) {
        return switch (exerciseType) {
            case READING -> "Choose the option that best matches the text.";
            case LISTENING -> "Select the most accurate listening response.";
            case SPEAKING -> "Provide a concise spoken response based on the prompt.";
            case WRITING -> "Write a short answer using relevant interview vocabulary.";
        };
    }

    private ExerciseSubtype toExerciseSubtype(String value) {
        if (value == null || value.isBlank()) {
            return ExerciseSubtype.FREE_TEXT;
        }

        try {
            return ExerciseSubtype.fromValue(value);
        } catch (IllegalArgumentException exception) {
            return ExerciseSubtype.FREE_TEXT;
        }
    }

    private ExerciseType inferExerciseType(ExerciseSubtype subtype) {
        return switch (subtype) {
            case MULTIPLE_CHOICE, LISTENING_CHOICE -> ExerciseType.READING;
            case SPEAKING_PROMPT -> ExerciseType.SPEAKING;
            case TRANSLATION, FILL_IN_BLANK, SENTENCE_BUILDING, FREE_TEXT -> ExerciseType.WRITING;
        };
    }

    private String buildFormat(ExerciseType type) {
        return switch (type) {
            case READING -> "Reading comprehension";
            case LISTENING -> "Listening response";
            case SPEAKING -> "Spoken response";
            case WRITING -> "Short written answer";
        };
    }

    private boolean isAiExercise(Exercise exercise) {
        return exercise.getQuestion() != null && exercise.getQuestion().startsWith("AI ");
    }
}
