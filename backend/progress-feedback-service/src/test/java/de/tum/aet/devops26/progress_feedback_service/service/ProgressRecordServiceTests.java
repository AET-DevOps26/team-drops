package de.tum.aet.devops26.progress_feedback_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.progress_feedback_service.model.ProgressRecord;
import de.tum.aet.devops26.progress_feedback_service.repository.ProgressRecordRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressRecordServiceTests {

    @Mock
    private ProgressRecordRepository progressRecordRepository;

    @Test
    void recordSubmittedAnswerCreatesScopedProgressRecord() {
        ProgressRecordService service = new ProgressRecordService(progressRecordRepository);

        when(progressRecordRepository.findByUserIdAndPlanIdAndTargetLanguage(42L, 1L, "German"))
            .thenReturn(Optional.empty());
        when(progressRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressRecord progressRecord = service.recordSubmittedAnswer(42L, 1L, "German", 80);

        assertThat(progressRecord.getUserId()).isEqualTo(42L);
        assertThat(progressRecord.getPlanId()).isEqualTo(1L);
        assertThat(progressRecord.getTargetLanguage()).isEqualTo("German");
        assertThat(progressRecord.getCompletedExercises()).isEqualTo(1);
        assertThat(progressRecord.getAverageScore()).isEqualTo(80.0);
        verify(progressRecordRepository, never()).findByUserId(42L);
    }

    @Test
    void recordSubmittedAnswerUpdatesOnlyMatchingLanguageRecord() {
        ProgressRecordService service = new ProgressRecordService(progressRecordRepository);
        ProgressRecord germanRecord = ProgressRecord.builder()
            .userId(42L)
            .planId(1L)
            .targetLanguage("German")
            .completedExercises(1)
            .totalExercises(1)
            .averageScore(80.0)
            .build();

        when(progressRecordRepository.findByUserIdAndPlanIdAndTargetLanguage(42L, 1L, "German"))
            .thenReturn(Optional.of(germanRecord));
        when(progressRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressRecord progressRecord = service.recordSubmittedAnswer(42L, 1L, "German", 60);

        assertThat(progressRecord.getTargetLanguage()).isEqualTo("German");
        assertThat(progressRecord.getCompletedExercises()).isEqualTo(2);
        assertThat(progressRecord.getAverageScore()).isEqualTo(70.0);
        verify(progressRecordRepository).findByUserIdAndPlanIdAndTargetLanguage(42L, 1L, "German");
    }

    @Test
    void findResponseOrEmptyReturnsInitialProgressForNewLanguage() {
        ProgressRecordService service = new ProgressRecordService(progressRecordRepository);
        when(progressRecordRepository.findByUserIdAndPlanIdAndTargetLanguage(42L, 1L, "English"))
            .thenReturn(Optional.empty());

        var response = service.findResponseOrEmpty(42L, 1L, "English");

        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(response.getCompletedExercises()).isZero();
        assertThat(response.getTotalExercises()).isZero();
        assertThat(response.getAverageScore()).isZero();
        assertThat(response.getRecentProgress()).isEmpty();
        assertThat(response.getRecentFinished()).isEmpty();
        assertThat(response.getTopLessons()).isEmpty();
    }
}
