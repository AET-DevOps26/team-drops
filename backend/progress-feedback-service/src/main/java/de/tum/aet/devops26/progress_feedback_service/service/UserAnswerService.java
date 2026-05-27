package de.tum.aet.devops26.progress_feedback_service.service;

import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.UserAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.model.UserAnswer;
import de.tum.aet.devops26.progress_feedback_service.repository.UserAnswerRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAnswerService {

    private static final double PLACEHOLDER_SCORE = 0.75;

    private final UserAnswerRepository userAnswerRepository;
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

    public UserAnswerResponse submitAnswer(SubmitAnswerRequest request) {
        UserAnswer userAnswer = UserAnswer.builder()
            .userId(request.getUserId())
            .exerciseId(request.getExerciseId())
            .answerText(request.getAnswerText())
            .score(PLACEHOLDER_SCORE)
            .build();

        UserAnswer savedAnswer = save(userAnswer);
        progressRecordService.recordSubmittedAnswer(savedAnswer.getUserId(), savedAnswer.getScore());

        return toResponse(savedAnswer);
    }

    private UserAnswerResponse toResponse(UserAnswer userAnswer) {
        return new UserAnswerResponse(
            userAnswer.getId(),
            userAnswer.getUserId(),
            userAnswer.getExerciseId(),
            userAnswer.getAnswerText(),
            userAnswer.getScore()
        );
    }
}
