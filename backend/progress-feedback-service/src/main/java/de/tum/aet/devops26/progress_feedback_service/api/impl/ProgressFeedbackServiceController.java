package de.tum.aet.devops26.progress_feedback_service.api.impl;

import de.tum.aet.devops26.progress_feedback_service.api.ProgressFeedbackServiceApi;
import de.tum.aet.devops26.progress_feedback_service.dto.FeedbackResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.ProgressResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.UserAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.service.FeedbackService;
import de.tum.aet.devops26.progress_feedback_service.service.ProgressRecordService;
import de.tum.aet.devops26.progress_feedback_service.service.UserAnswerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProgressFeedbackServiceController implements ProgressFeedbackServiceApi {

    private final FeedbackService feedbackService;
    private final UserAnswerService userAnswerService;
    private final ProgressRecordService progressRecordService;

    @Override
    public ResponseEntity<FeedbackResponse> getFeedbackByAnswerId(Long answerId) {
        return feedbackService.findResponseByAnswerId(answerId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/answers/user/{userId}")
    public ResponseEntity<List<UserAnswerResponse>> getAnswersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userAnswerService.findResponsesByUserId(userId));
    }

    @Override
    public ResponseEntity<ProgressResponse> getProgressByUserId(Long userId) {
        return progressRecordService.findResponseByUserId(userId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(SubmitAnswerRequest submitAnswerRequest) {
        return ResponseEntity.ok(userAnswerService.submitAnswer(submitAnswerRequest));
    }
}
