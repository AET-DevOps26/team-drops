package de.tum.aet.devops26.learning_service.repository;

import de.tum.aet.devops26.learning_service.model.Exercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByLessonId(Long lessonId);
}
