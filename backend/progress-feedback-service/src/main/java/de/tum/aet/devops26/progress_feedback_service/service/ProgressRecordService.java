package de.tum.aet.devops26.progress_feedback_service.service;

import de.tum.aet.devops26.progress_feedback_service.model.ProgressRecord;
import de.tum.aet.devops26.progress_feedback_service.repository.ProgressRecordRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProgressRecordService {

    private final ProgressRecordRepository progressRecordRepository;

    public ProgressRecord save(ProgressRecord progressRecord) {
        return progressRecordRepository.save(progressRecord);
    }

    public List<ProgressRecord> findAll() {
        return progressRecordRepository.findAll();
    }

    public Optional<ProgressRecord> findById(Long id) {
        return progressRecordRepository.findById(id);
    }

    public Optional<ProgressRecord> findByUserId(Long userId) {
        return progressRecordRepository.findByUserId(userId);
    }

    public void deleteById(Long id) {
        progressRecordRepository.deleteById(id);
    }
}
