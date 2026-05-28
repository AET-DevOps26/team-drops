package de.tum.aet.devops26.progress_feedback_service.service;

import de.tum.aet.devops26.progress_feedback_service.dto.FeedbackResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.LearningStatus;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.UserAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.model.Feedback;
import de.tum.aet.devops26.progress_feedback_service.model.UserAnswer;
import de.tum.aet.devops26.progress_feedback_service.repository.FeedbackRepository;
import de.tum.aet.devops26.progress_feedback_service.repository.UserAnswerRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAnswerService {

    private static final int PLACEHOLDER_SCORE = 75;
    private static final int MAX_SCORE = 100;

    private final UserAnswerRepository userAnswerRepository;
    private final FeedbackRepository feedbackRepository;
    private final ProgressRecordService progressRecordService;

    public UserAnswer save(UserAnswer userAnswer) {
        return userAnswerRepository.save(userAnswer);
    }

    public List<UserAnswer> findAll() {
        return userAnswerRepository.findAll();
    }

    public Optional<UserAnswer> findById(Long id) {
        return userAnswerRepository.findById(id);
    }

    public List<UserAnswer> findByUserId(Long userId) {
        return userAnswerRepository.findByUserId(userId);
    }

    public List<UserAnswer> findByExerciseId(Long exerciseId) {
        return userAnswerRepository.findByExerciseId(exerciseId);
    }

    public void deleteById(Long id) {
        userAnswerRepository.deleteById(id);
    }

    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {
        UserAnswer userAnswer = UserAnswer.builder()
            .userId(request.getUserId())
            .exerciseId(request.getExerciseId())
            .answerText(request.getAnswerText())
            .score((double) PLACEHOLDER_SCORE)
            .build();

        UserAnswer savedAnswer = save(userAnswer);
        Feedback savedFeedback = feedbackRepository.save(Feedback.builder()
            .answerId(savedAnswer.getId())
            .message("Answer saved and scored with placeholder feedback.")
            .weakArea("specificity")
            .build());
        progressRecordService.recordSubmittedAnswer(savedAnswer.getUserId(), PLACEHOLDER_SCORE);

        SubmitAnswerResponse response = new SubmitAnswerResponse(
            toResponse(savedAnswer),
            LearningStatus.FINISHED,
            PLACEHOLDER_SCORE,
            PLACEHOLDER_SCORE
        );
        response.setFeedback(toFeedbackResponse(savedFeedback));
        return response;
    }

    private UserAnswerResponse toResponse(UserAnswer userAnswer) {
        return new UserAnswerResponse(
            userAnswer.getId(),
            userAnswer.getUserId(),
            userAnswer.getExerciseId(),
            userAnswer.getAnswerText(),
            userAnswer.getScore() == null ? null : userAnswer.getScore().intValue(),
            MAX_SCORE,
            userAnswer.getScore() != null && userAnswer.getScore() >= 60,
            OffsetDateTime.ofInstant(userAnswer.getSubmittedAt(), ZoneOffset.UTC)
        );
    }

    private FeedbackResponse toFeedbackResponse(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse(
            feedback.getId(),
            feedback.getAnswerId(),
            feedback.getMessage(),
            OffsetDateTime.ofInstant(feedback.getCreatedAt(), ZoneOffset.UTC)
        );
        response.setWeakArea(feedback.getWeakArea());
        return response;
    }
}
