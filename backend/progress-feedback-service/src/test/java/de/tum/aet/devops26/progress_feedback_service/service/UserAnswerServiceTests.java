package de.tum.aet.devops26.progress_feedback_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.progress_feedback_service.dto.AnswerClientContext;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient.ExerciseContext;
import de.tum.aet.devops26.progress_feedback_service.model.Feedback;
import de.tum.aet.devops26.progress_feedback_service.model.UserAnswer;
import de.tum.aet.devops26.progress_feedback_service.repository.FeedbackRepository;
import de.tum.aet.devops26.progress_feedback_service.repository.UserAnswerRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAnswerServiceTests {

    @Mock
    private UserAnswerRepository userAnswerRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private ProgressRecordService progressRecordService;

    @Mock
    private LearningServiceClient learningServiceClient;

    @Mock
    private GenAiWritingClient genAiWritingClient;

    @Mock
    private ListeningContentService listeningContentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitAnswerEvaluatesWritingAndReturnsPersistedFeedback() {
        UserAnswerService service = new UserAnswerService(
            userAnswerRepository,
            feedbackRepository,
            progressRecordService,
            learningServiceClient,
            genAiWritingClient,
            listeningContentService,
            objectMapper
        );
        SubmitAnswerRequest request = new SubmitAnswerRequest(
            42L,
            7L,
            "Je voudrai un cafe",
            new AnswerClientContext()
            .lessonId(3L)
            .planId(1L)
            .targetLanguage("French")
            .level("A2")
        );

        when(learningServiceClient.getExercise(3L, 7L, "French")).thenReturn(new ExerciseContext(
            7L,
            "writing",
            "translation",
            "Translate: I would like a coffee",
            "Je voudrais un cafe",
            "A2"
        ));
        when(genAiWritingClient.evaluate(any())).thenReturn(new WritingEvaluationResponse(
            8.5,
            true,
            "Use the conditional form.",
            "verb conjugation",
            "Je voudrais un cafe"
        ));
        when(userAnswerRepository.save(any())).thenAnswer(invocation -> {
            UserAnswer answer = invocation.getArgument(0);
            answer.setId(11L);
            answer.setSubmittedAt(Instant.parse("2026-06-01T10:00:00Z"));
            return answer;
        });
        when(feedbackRepository.save(any())).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(12L);
            feedback.setCreatedAt(Instant.parse("2026-06-01T10:00:00Z"));
            return feedback;
        });

        SubmitAnswerResponse response = service.submitAnswer(request);

        assertThat(response.getAnswer().getScore()).isEqualTo(85);
        assertThat(response.getFeedback()).isNotNull();
        assertThat(response.getFeedback().getWeakArea()).isEqualTo("verb conjugation");
        assertThat(response.getFeedback().getCorrectedAnswer()).isEqualTo("Je voudrais un cafe");
        verify(progressRecordService).recordSubmittedAnswer(42L, 85);
    }

    @Test
    void submitAnswerScoresListeningWithoutWritingEvaluation() {
        UserAnswerService service = new UserAnswerService(
            userAnswerRepository,
            feedbackRepository,
            progressRecordService,
            learningServiceClient,
            genAiWritingClient,
            listeningContentService,
            objectMapper
        );
        SubmitAnswerRequest request = new SubmitAnswerRequest(
            42L,
            7L,
            "{\"0\":\"Im Cafe\",\"1\":\"Mit dem Bus\"}",
            new AnswerClientContext()
            .lessonId(3L)
            .planId(1L)
            .targetLanguage("German")
            .level("A2")
        );

        when(learningServiceClient.getExercise(3L, 7L, "German")).thenReturn(new ExerciseContext(
            7L,
            "listening",
            "listening_choice",
            "AI listening exercise 1: cafe conversation",
            "Select the most accurate listening response.",
            "A2"
        ));
        when(listeningContentService.scoreAnswers(eq(7L), any(Map.class)))
            .thenReturn(new ListeningContentService.ScoreResult(50, 1, 2));
        when(userAnswerRepository.save(any())).thenAnswer(invocation -> {
            UserAnswer answer = invocation.getArgument(0);
            answer.setId(11L);
            answer.setSubmittedAt(Instant.parse("2026-06-01T10:00:00Z"));
            return answer;
        });
        when(feedbackRepository.save(any())).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(12L);
            feedback.setCreatedAt(Instant.parse("2026-06-01T10:00:00Z"));
            return feedback;
        });

        SubmitAnswerResponse response = service.submitAnswer(request);

        assertThat(response.getAnswer().getScore()).isEqualTo(50);
        assertThat(response.getFeedback()).isNotNull();
        assertThat(response.getFeedback().getMessage()).isEqualTo("You got 1 out of 2 correct (50%).");
        verify(genAiWritingClient, never()).evaluate(any());
        verify(progressRecordService).recordSubmittedAnswer(42L, 50);
    }
}
