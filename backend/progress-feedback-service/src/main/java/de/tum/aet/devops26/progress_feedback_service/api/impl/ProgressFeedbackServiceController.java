package de.tum.aet.devops26.progress_feedback_service.api.impl;

import de.tum.aet.devops26.progress_feedback_service.api.ProgressFeedbackServiceApi;
import de.tum.aet.devops26.progress_feedback_service.dto.FeedbackResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.ListeningContentResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.ProgressResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.ProgressListeningGenerateRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.ProgressListeningOption;
import de.tum.aet.devops26.progress_feedback_service.dto.ProgressListeningQuestion;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.UserAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiListeningClient;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient;
import de.tum.aet.devops26.progress_feedback_service.service.FeedbackService;
import de.tum.aet.devops26.progress_feedback_service.service.ListeningContentService;
import de.tum.aet.devops26.progress_feedback_service.service.ProgressRecordService;
import de.tum.aet.devops26.progress_feedback_service.service.UserAnswerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProgressFeedbackServiceController implements ProgressFeedbackServiceApi {

    private final FeedbackService feedbackService;
    private final UserAnswerService userAnswerService;
    private final ProgressRecordService progressRecordService;
    private final ListeningContentService listeningContentService;
    private final LearningServiceClient learningServiceClient;

    @Override
    public ResponseEntity<FeedbackResponse> getFeedbackByAnswerId(Long answerId) {
        return feedbackService.findResponseByAnswerId(answerId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<UserAnswerResponse>> getAnswersByUserId(
        Long userId,
        Long planId,
        String targetLanguage
    ) {
        return ResponseEntity.ok(userAnswerService.findResponsesByUserId(userId, planId, targetLanguage));
    }

    @Override
    public ResponseEntity<ProgressResponse> getProgressByUserId(Long userId, Long planId, String targetLanguage) {
        return progressRecordService.findResponseByUserId(userId, planId, targetLanguage)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(SubmitAnswerRequest submitAnswerRequest) {
        return ResponseEntity.ok(userAnswerService.submitAnswer(submitAnswerRequest));
    }

    @Override
    public ResponseEntity<ListeningContentResponse> generateListeningContent(ProgressListeningGenerateRequest request) {
        LearningServiceClient.ExerciseContext exercise = learningServiceClient.getExercise(
            request.getLessonId(), request.getExerciseId());

        String language = request.getTargetLanguage() != null && !request.getTargetLanguage().isBlank()
            ? request.getTargetLanguage() : "German";
        String level = request.getLevel() != null && !request.getLevel().isBlank()
            ? request.getLevel()
            : (exercise.difficulty() != null ? exercise.difficulty() : "A2");

        GenAiListeningClient.ListeningGenerateResponse genAiResponse = listeningContentService.generateOrLoad(
            request.getExerciseId(),
            language,
            level,
            exercise.question()
        );

        return ResponseEntity.ok(buildListeningResponse(genAiResponse));
    }

    private ListeningContentResponse buildListeningResponse(GenAiListeningClient.ListeningGenerateResponse r) {
        List<ProgressListeningQuestion> questions = r.questions().stream()
            .map(q -> {
                ProgressListeningQuestion dto = new ProgressListeningQuestion();
                dto.setQuestion(q.question());
                dto.setExplanation(q.explanation());
                dto.setOptions(q.options().stream()
                    .map(opt -> {
                        ProgressListeningOption optDto = new ProgressListeningOption();
                        optDto.setText(opt.text());
                        return optDto;
                    })
                    .toList());
                return dto;
            })
            .toList();

        ListeningContentResponse response = new ListeningContentResponse();
        response.setScript(r.script());
        response.setQuestions(questions);
        response.setAudioB64(r.scriptAudioB64());
        return response;
    }
}
