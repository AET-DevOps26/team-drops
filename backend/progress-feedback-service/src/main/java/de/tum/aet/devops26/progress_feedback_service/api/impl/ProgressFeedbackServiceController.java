package de.tum.aet.devops26.progress_feedback_service.api.impl;

import de.tum.aet.devops26.progress_feedback_service.api.ProgressFeedbackServiceApi;
import de.tum.aet.devops26.progress_feedback_service.dto.ProgressResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.UserAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.service.ProgressRecordService;
import de.tum.aet.devops26.progress_feedback_service.service.UserAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProgressFeedbackServiceController implements ProgressFeedbackServiceApi {

    private final UserAnswerService userAnswerService;
    private final ProgressRecordService progressRecordService;

    @Override
    public ResponseEntity<ProgressResponse> getProgressByUserId(Long userId) {
        return progressRecordService.findResponseByUserId(userId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserAnswerResponse> submitAnswer(SubmitAnswerRequest submitAnswerRequest) {
        return ResponseEntity.ok(userAnswerService.submitAnswer(submitAnswerRequest));
    }
}
