package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.ExerciseResponse;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.repository.ExerciseRepository;
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
        return exerciseRepository.findByLessonId(lessonId);
    }

    public void deleteById(Long id) {
        exerciseRepository.deleteById(id);
    }

    public ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(
            exercise.getId(),
            exercise.getLessonId(),
            exercise.getType(),
            exercise.getQuestion(),
            exercise.getDifficulty(),
            exercise.getExpectedAnswer()
        );
    }
}
