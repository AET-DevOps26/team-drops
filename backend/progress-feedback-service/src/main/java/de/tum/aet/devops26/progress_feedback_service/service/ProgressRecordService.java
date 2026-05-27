package de.tum.aet.devops26.progress_feedback_service.service;

import de.tum.aet.devops26.progress_feedback_service.dto.ProgressResponse;
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

    public Optional<ProgressResponse> findResponseByUserId(Long userId) {
        return findByUserId(userId).map(this::toResponse);
    }

    public ProgressRecord recordSubmittedAnswer(Long userId, Double score) {
        ProgressRecord progressRecord = findByUserId(userId)
            .orElseGet(() -> ProgressRecord.builder()
                .userId(userId)
                .completedExercises(0)
                .totalExercises(0)
                .averageScore(0.0)
                .build());

        int previousCompleted = progressRecord.getCompletedExercises();
        int completedExercises = previousCompleted + 1;
        double previousAverage = progressRecord.getAverageScore() == null ? 0.0 : progressRecord.getAverageScore();
        double averageScore = ((previousAverage * previousCompleted) + score) / completedExercises;

        progressRecord.setCompletedExercises(completedExercises);
        progressRecord.setTotalExercises(Math.max(progressRecord.getTotalExercises(), completedExercises));
        progressRecord.setAverageScore(averageScore);

        return save(progressRecord);
    }

    private ProgressResponse toResponse(ProgressRecord progressRecord) {
        return new ProgressResponse(
            progressRecord.getId(),
            progressRecord.getUserId(),
            progressRecord.getCompletedExercises(),
            progressRecord.getTotalExercises(),
            progressRecord.getAverageScore()
        );
    }
}
