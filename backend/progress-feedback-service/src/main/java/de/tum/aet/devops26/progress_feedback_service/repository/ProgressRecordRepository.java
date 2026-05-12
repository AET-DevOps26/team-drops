package de.tum.aet.devops26.progress_feedback_service.repository;

import de.tum.aet.devops26.progress_feedback_service.model.ProgressRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, Long> {

    Optional<ProgressRecord> findByUserId(Long userId);
}
