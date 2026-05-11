package de.tum.aet.devops26.progress_feedback_service.repository;

import de.tum.aet.devops26.progress_feedback_service.model.UserAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    List<UserAnswer> findByUserId(Long userId);

    List<UserAnswer> findByExerciseId(Long exerciseId);
}
