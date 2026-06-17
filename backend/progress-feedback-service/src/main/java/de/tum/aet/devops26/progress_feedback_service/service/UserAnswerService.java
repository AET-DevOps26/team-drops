package de.tum.aet.devops26.progress_feedback_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.progress_feedback_service.dto.FeedbackResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.LearningStatus;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.UserAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationRequest;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient.ExerciseContext;
import de.tum.aet.devops26.progress_feedback_service.model.Feedback;
import de.tum.aet.devops26.progress_feedback_service.model.UserAnswer;
import de.tum.aet.devops26.progress_feedback_service.repository.FeedbackRepository;
import de.tum.aet.devops26.progress_feedback_service.repository.UserAnswerRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserAnswerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAnswerService.class);
    private static final int MAX_SCORE = 100;

    private final UserAnswerRepository userAnswerRepository;
    private final FeedbackRepository feedbackRepository;
    private final ProgressRecordService progressRecordService;
    private final LearningServiceClient learningServiceClient;
    private final GenAiWritingClient genAiWritingClient;
    private final ListeningContentService listeningContentService;
    private final ObjectMapper objectMapper;

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

    public List<UserAnswerResponse> findResponsesByUserId(Long userId) {
        return findByUserId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<UserAnswer> findByExerciseId(Long exerciseId) {
        return userAnswerRepository.findByExerciseId(exerciseId);
    }

    public void deleteById(Long id) {
        userAnswerRepository.deleteById(id);
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {
        if (request.getClientContext() == null || request.getClientContext().getLessonId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "client_context.lesson_id is required.");
        }

        ExerciseContext exercise = learningServiceClient.getExercise(
            request.getClientContext().getLessonId(),
            request.getExerciseId()
        );

        if ("LISTENING".equalsIgnoreCase(exercise.type())) {
            return submitListeningAnswer(request, exercise);
        }

        return submitWritingAnswer(request, exercise);
    }

    private SubmitAnswerResponse submitWritingAnswer(SubmitAnswerRequest request, ExerciseContext exercise) {
        WritingEvaluationResponse evaluation = genAiWritingClient.evaluate(new WritingEvaluationRequest(
            request.getUserId(),
            request.getExerciseId(),
            exercise.subtype() == null ? exercise.type() : exercise.subtype(),
            exercise.question(),
            exercise.expectedAnswer(),
            request.getAnswerText(),
            valueOrDefault(request.getClientContext().getTargetLanguage(), "English"),
            valueOrDefault(request.getClientContext().getLevel(), exercise.difficulty())
        ));
        int score = Math.max(0, Math.min(MAX_SCORE, (int) Math.round(evaluation.score() * 10)));

        UserAnswer savedAnswer = save(UserAnswer.builder()
            .userId(request.getUserId())
            .exerciseId(request.getExerciseId())
            .answerText(request.getAnswerText())
            .score((double) score)
            .build());

        Feedback savedFeedback = feedbackRepository.save(Feedback.builder()
            .answerId(savedAnswer.getId())
            .message(evaluation.message())
            .weakArea(evaluation.weakArea())
            .correctedAnswer(evaluation.correctedAnswer())
            .build());
        progressRecordService.recordSubmittedAnswer(savedAnswer.getUserId(), score);

        SubmitAnswerResponse response = new SubmitAnswerResponse(
            toResponse(savedAnswer), LearningStatus.FINISHED, score, score);
        response.setFeedback(toFeedbackResponse(savedFeedback));
        return response;
    }

    private SubmitAnswerResponse submitListeningAnswer(SubmitAnswerRequest request, ExerciseContext exercise) {
        Map<Integer, String> selections;
        try {
            selections = objectMapper.readValue(request.getAnswerText(), new TypeReference<>() {});
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse listening answer_text as JSON for exercise {}: {}",
                request.getExerciseId(), exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "answer_text for a listening exercise must be a JSON object mapping question index to selected option text.");
        }

        ListeningContentService.ScoreResult scoreResult = listeningContentService.scoreAnswers(request.getExerciseId(), selections);
        int score = scoreResult.score();
        String feedbackMessage = score == 100
            ? "Excellent! All answers correct."
            : String.format("You got %d out of %d correct (%d%%).", scoreResult.correct(), scoreResult.total(), scoreResult.score());

        UserAnswer savedAnswer = save(UserAnswer.builder()
            .userId(request.getUserId())
            .exerciseId(request.getExerciseId())
            .answerText(request.getAnswerText())
            .score((double) score)
            .build());

        Feedback savedFeedback = feedbackRepository.save(Feedback.builder()
            .answerId(savedAnswer.getId())
            .message(feedbackMessage)
            .build());
        progressRecordService.recordSubmittedAnswer(savedAnswer.getUserId(), score);

        SubmitAnswerResponse response = new SubmitAnswerResponse(
            toResponse(savedAnswer), LearningStatus.FINISHED, score, score);
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
        response.setCorrectedAnswer(feedback.getCorrectedAnswer());
        return response;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
