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

    public Optional<ProgressResponse> findResponseByUserId(Long userId, Long planId, String targetLanguage) {
        if (planId == null && isBlank(targetLanguage)) {
            return findResponseByUserId(userId);
        }

        String normalizedLanguage = normalizeLanguage(targetLanguage);
        if (planId == null) {
            List<ProgressRecord> languageRecords = progressRecordRepository.findByUserIdAndTargetLanguage(
                userId,
                normalizedLanguage
            );
            if (!languageRecords.isEmpty()) {
                return Optional.of(toCombinedResponse(userId, languageRecords));
            }

            return progressRecordRepository.findFirstByUserIdAndPlanIdIsNullAndTargetLanguageIsNull(userId)
                .map(this::toResponse);
        }

        Optional<ProgressRecord> exactRecord = planId == null
            ? progressRecordRepository.findByUserId(userId)
            : progressRecordRepository.findByUserIdAndPlanIdAndTargetLanguage(
                userId,
                planId,
                normalizedLanguage
            );

        return exactRecord
            .or(() -> progressRecordRepository.findFirstByUserIdAndPlanIdIsNullAndTargetLanguageIsNull(userId))
            .map(this::toResponse);
    }

    public ProgressRecord recordSubmittedAnswer(Long userId, Integer score) {
        return recordSubmittedAnswer(userId, null, null, score);
    }

    public ProgressRecord recordSubmittedAnswer(Long userId, Long planId, String targetLanguage, Integer score) {
        String normalizedLanguage = normalizeLanguage(targetLanguage);
        ProgressRecord progressRecord = findProgressRecord(userId, planId, normalizedLanguage)
            .orElseGet(() -> ProgressRecord.builder()
                .userId(userId)
                .planId(planId)
                .targetLanguage(normalizedLanguage)
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

    private Optional<ProgressRecord> findProgressRecord(Long userId, Long planId, String targetLanguage) {
        if (planId == null && isBlank(targetLanguage)) {
            return findByUserId(userId);
        }

        if (planId == null) {
            return findByUserId(userId);
        }

        return progressRecordRepository.findByUserIdAndPlanIdAndTargetLanguage(userId, planId, targetLanguage);
    }

    private ProgressResponse toResponse(ProgressRecord progressRecord) {
        return new ProgressResponse(
            progressRecord.getUserId(),
            progressRecord.getCompletedExercises(),
            progressRecord.getTotalExercises(),
            (int) Math.round(progressRecord.getAverageScore() == null ? 0.0 : progressRecord.getAverageScore()),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private ProgressResponse toCombinedResponse(Long userId, List<ProgressRecord> records) {
        int completedExercises = records.stream()
            .mapToInt(record -> record.getCompletedExercises() == null ? 0 : record.getCompletedExercises())
            .sum();
        int totalExercises = records.stream()
            .mapToInt(record -> record.getTotalExercises() == null ? 0 : record.getTotalExercises())
            .sum();
        double weightedScore = records.stream()
            .mapToDouble(record -> {
                int completed = record.getCompletedExercises() == null ? 0 : record.getCompletedExercises();
                double average = record.getAverageScore() == null ? 0.0 : record.getAverageScore();
                return completed * average;
            })
            .sum();
        int averageScore = completedExercises == 0 ? 0 : (int) Math.round(weightedScore / completedExercises);

        return new ProgressResponse(
            userId,
            completedExercises,
            totalExercises,
            averageScore,
            List.of(),
            List.of(),
            List.of()
        );
    }

    private String normalizeLanguage(String targetLanguage) {
        return isBlank(targetLanguage) ? null : targetLanguage.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
