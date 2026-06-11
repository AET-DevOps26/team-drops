package de.tum.aet.devops26.learning_service.repository;

import de.tum.aet.devops26.learning_service.model.LearningPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {

    List<LearningPlan> findByUserId(Long userId);

    Optional<LearningPlan> findFirstByUserIdAndTitle(Long userId, String title);
}
