package de.tum.aet.devops26.progress_feedback_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.progress_feedback_service.dto.FeedbackResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.LearningStatus;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitSpeakingAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.UserAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiSpeakingClient;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiSpeakingClient.SpeakingEvaluationRequest;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiSpeakingClient.SpeakingEvaluationResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationRequest;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient.ExerciseContext;
import de.tum.aet.devops26.progress_feedback_service.integration.UserServiceClient;
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
import org.springframework.web.multipart.MultipartFile;
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
    private final GenAiSpeakingClient genAiSpeakingClient;
    private final UserServiceClient userServiceClient;
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

    public List<UserAnswerResponse> findResponsesByUserId(Long userId, Long planId, String targetLanguage) {
        if (planId == null && isBlank(targetLanguage)) {
            return findResponsesByUserId(userId);
        }

        String normalizedLanguage = normalizeLanguage(targetLanguage);
        return findByUserId(userId).stream()
            .filter(answer -> matchesPlanOrLegacy(answer, planId))
            .filter(answer -> matchesLanguageOrLegacy(answer, normalizedLanguage))
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
            request.getExerciseId(),
            request.getClientContext().getTargetLanguage()
        );

        if ("LISTENING".equalsIgnoreCase(exercise.type()) || containsListening(exercise.subtype())) {
            return submitListeningAnswer(request);
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
        int score = toPercentScore(evaluation.score());

        UserAnswer savedAnswer = save(UserAnswer.builder()
            .userId(request.getUserId())
            .exerciseId(request.getExerciseId())
            .planId(request.getClientContext().getPlanId())
            .targetLanguage(normalizeLanguage(request.getClientContext().getTargetLanguage()))
            .answerText(request.getAnswerText())
            .score((double) score)
            .build());

        Feedback savedFeedback = feedbackRepository.save(Feedback.builder()
            .answerId(savedAnswer.getId())
            .message(evaluation.message())
            .weakArea(evaluation.weakArea())
            .correctedAnswer(evaluation.correctedAnswer())
            .build());
        var progressRecord = progressRecordService.recordSubmittedAnswer(
            savedAnswer.getUserId(),
            savedAnswer.getPlanId(),
            savedAnswer.getTargetLanguage(),
            score
        );

        SubmitAnswerResponse response = new SubmitAnswerResponse(
            toResponse(savedAnswer), LearningStatus.FINISHED, score, toProgressValue(progressRecord.getAverageScore()));
        response.setFeedback(toFeedbackResponse(savedFeedback));
        return response;
    }

    private SubmitAnswerResponse submitListeningAnswer(SubmitAnswerRequest request) {
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
            .planId(request.getClientContext().getPlanId())
            .targetLanguage(normalizeLanguage(request.getClientContext().getTargetLanguage()))
            .answerText(request.getAnswerText())
            .score((double) score)
            .build());

        Feedback savedFeedback = feedbackRepository.save(Feedback.builder()
            .answerId(savedAnswer.getId())
            .message(feedbackMessage)
            .build());
        var progressRecord = progressRecordService.recordSubmittedAnswer(
            savedAnswer.getUserId(),
            savedAnswer.getPlanId(),
            savedAnswer.getTargetLanguage(),
            score
        );

        SubmitAnswerResponse response = new SubmitAnswerResponse(
            toResponse(savedAnswer), LearningStatus.FINISHED, score, toProgressValue(progressRecord.getAverageScore()));
        response.setFeedback(toFeedbackResponse(savedFeedback));
        return response;
    }

    @Transactional
    public SubmitSpeakingAnswerResponse submitSpeakingAnswer(
            Long userId,
            Long exerciseId,
            Long lessonId,
            Long planId,
            String targetLanguage,
            String level,
            MultipartFile audio) {
        if (lessonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lesson_id is required.");
        }
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "audio is required.");
        }
        if (isBlank(targetLanguage)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target_language is required.");
        }

        Long resolvedUserId = userServiceClient.resolveSubmittedUserId(userId);
        String normalizedLanguage = normalizeLanguage(targetLanguage);
        ExerciseContext exercise = learningServiceClient.getExercise(lessonId, exerciseId, normalizedLanguage);
        if (!isSpeakingExercise(exercise)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise is not a speaking exercise.");
        }

        SpeakingEvaluationResponse evaluation = genAiSpeakingClient.evaluate(new SpeakingEvaluationRequest(
            resolvedUserId,
            exerciseId,
            readAudioBytes(audio),
            audio.getOriginalFilename(),
            audio.getContentType(),
            valueOrDefault(exercise.subtype(), exercise.type()),
            exercise.question(),
            exercise.expectedAnswer(),
            normalizedLanguage,
            valueOrDefault(level, exercise.difficulty())
        ));
        int score = toPercentScore(evaluation.score());

        UserAnswer savedAnswer = save(UserAnswer.builder()
            .userId(resolvedUserId)
            .exerciseId(exerciseId)
            .planId(planId)
            .targetLanguage(normalizedLanguage)
            .answerText(evaluation.transcription())
            .score((double) score)
            .build());

        Feedback savedFeedback = feedbackRepository.save(Feedback.builder()
            .answerId(savedAnswer.getId())
            .message(evaluation.message())
            .weakArea(evaluation.weakArea())
            .correctedAnswer(evaluation.correctedAnswer())
            .build());
        var progressRecord = progressRecordService.recordSubmittedAnswer(
            savedAnswer.getUserId(),
            savedAnswer.getPlanId(),
            savedAnswer.getTargetLanguage(),
            score
        );

        SubmitSpeakingAnswerResponse response = new SubmitSpeakingAnswerResponse();
        response.setAnswer(toResponse(savedAnswer));
        response.setFeedback(toFeedbackResponse(savedFeedback));
        response.setExerciseStatus(LearningStatus.FINISHED);
        response.setLessonProgress(score);
        response.setPlanProgress(toProgressValue(progressRecord.getAverageScore()));
        response.setTranscription(evaluation.transcription());
        response.setFeedbackAudioB64(evaluation.feedbackAudioB64());
        return response;
    }

    private UserAnswerResponse toResponse(UserAnswer userAnswer) {
        UserAnswerResponse response = new UserAnswerResponse(
            userAnswer.getId(),
            userAnswer.getUserId(),
            userAnswer.getExerciseId(),
            userAnswer.getAnswerText(),
            userAnswer.getScore() == null ? null : userAnswer.getScore().intValue(),
            MAX_SCORE,
            userAnswer.getScore() != null && userAnswer.getScore() >= 60,
            OffsetDateTime.ofInstant(userAnswer.getSubmittedAt(), ZoneOffset.UTC)
        );
        response.setPlanId(userAnswer.getPlanId());
        response.setTargetLanguage(userAnswer.getTargetLanguage());
        return response;
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

    private int toPercentScore(double score) {
        return Math.max(0, Math.min(MAX_SCORE, (int) Math.round(score * 10)));
    }

    private boolean isSpeakingExercise(ExerciseContext exercise) {
        return containsSpeaking(exercise.type()) || containsSpeaking(exercise.subtype());
    }

    private boolean containsSpeaking(String value) {
        return value != null && value.toLowerCase().contains("speaking");
    }

    private boolean containsListening(String value) {
        return value != null && value.toLowerCase().contains("listening");
    }

    private byte[] readAudioBytes(MultipartFile audio) {
        try {
            return audio.getBytes();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded audio.", exception);
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean matchesLanguageOrLegacy(UserAnswer answer, String targetLanguage) {
        return answer.getTargetLanguage() == null
            || targetLanguage == null
            || answer.getTargetLanguage().equalsIgnoreCase(targetLanguage);
    }

    private boolean matchesPlanOrLegacy(UserAnswer answer, Long planId) {
        return answer.getPlanId() == null
            || planId == null
            || answer.getPlanId().equals(planId);
    }

    private String normalizeLanguage(String targetLanguage) {
        return isBlank(targetLanguage) ? null : targetLanguage.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int toProgressValue(Double averageScore) {
        return Math.max(0, Math.min(MAX_SCORE, (int) Math.round(averageScore == null ? 0.0 : averageScore)));
    }
}
