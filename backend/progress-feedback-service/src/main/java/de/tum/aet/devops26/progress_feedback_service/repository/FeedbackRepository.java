package de.tum.aet.devops26.progress_feedback_service.repository;

import de.tum.aet.devops26.progress_feedback_service.model.Feedback;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByAnswerId(Long answerId);
}
