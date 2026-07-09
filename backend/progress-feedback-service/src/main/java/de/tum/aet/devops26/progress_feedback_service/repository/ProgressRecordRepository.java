package de.tum.aet.devops26.progress_feedback_service.repository;

import de.tum.aet.devops26.progress_feedback_service.model.ProgressRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, Long> {

    Optional<ProgressRecord> findByUserId(Long userId);

    Optional<ProgressRecord> findByUserIdAndPlanIdAndTargetLanguage(Long userId, Long planId, String targetLanguage);

    Optional<ProgressRecord> findFirstByUserIdAndPlanIdIsNullAndTargetLanguageIsNull(Long userId);

    List<ProgressRecord> findByUserIdAndTargetLanguage(Long userId, String targetLanguage);

    List<ProgressRecord> findByUserIdAndPlanId(Long userId, Long planId);
}
