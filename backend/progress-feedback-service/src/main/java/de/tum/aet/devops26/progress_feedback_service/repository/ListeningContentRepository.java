package de.tum.aet.devops26.progress_feedback_service.repository;

import de.tum.aet.devops26.progress_feedback_service.model.ListeningContent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListeningContentRepository extends JpaRepository<ListeningContent, Long> {

    Optional<ListeningContent> findByExerciseId(Long exerciseId);
}
