package de.tum.aet.devops26.progress_feedback_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.progress_feedback_service.dto.AnswerClientContext;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerRequest;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.dto.SubmitSpeakingAnswerResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiSpeakingClient;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiSpeakingClient.SpeakingEvaluationResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiWritingClient.WritingEvaluationResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient;
import de.tum.aet.devops26.progress_feedback_service.integration.LearningServiceClient.ExerciseContext;
import de.tum.aet.devops26.progress_feedback_service.integration.UserServiceClient;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    private GenAiSpeakingClient genAiSpeakingClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ListeningContentService listeningContentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitAnswerEvaluatesWritingAndReturnsPersistedFeedback() {
        UserAnswerService service = newService();
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

        when(learningServiceClient.getExercise(3L, 7L)).thenReturn(new ExerciseContext(
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
        mockAnswerAndFeedbackSaves(11L, 12L, "2026-06-01T10:00:00Z");

        SubmitAnswerResponse response = service.submitAnswer(request);

        assertThat(response.getAnswer().getScore()).isEqualTo(85);
        assertThat(response.getFeedback()).isNotNull();
        assertThat(response.getFeedback().getWeakArea()).isEqualTo("verb conjugation");
        assertThat(response.getFeedback().getCorrectedAnswer()).isEqualTo("Je voudrais un cafe");
        verify(progressRecordService).recordSubmittedAnswer(42L, 85);
    }

    @Test
    void submitAnswerScoresListeningWithoutWritingEvaluation() {
        UserAnswerService service = newService();
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

        when(learningServiceClient.getExercise(3L, 7L)).thenReturn(new ExerciseContext(
            7L,
            "listening",
            "listening_choice",
            "AI listening exercise 1: cafe conversation",
            "Select the most accurate listening response.",
            "A2"
        ));
        when(listeningContentService.scoreAnswers(eq(7L), any(Map.class)))
            .thenReturn(new ListeningContentService.ScoreResult(50, 1, 2));
        mockAnswerAndFeedbackSaves(11L, 12L, "2026-06-01T10:00:00Z");

        SubmitAnswerResponse response = service.submitAnswer(request);

        assertThat(response.getAnswer().getScore()).isEqualTo(50);
        assertThat(response.getFeedback()).isNotNull();
        assertThat(response.getFeedback().getMessage()).isEqualTo("You got 1 out of 2 correct (50%).");
        verify(genAiWritingClient, never()).evaluate(any());
        verify(progressRecordService).recordSubmittedAnswer(42L, 50);
    }

    @Test
    void submitSpeakingAnswerEvaluatesAndReturnsPersistedFeedback() {
        UserAnswerService service = newService();

        whenSpeakingExercise();
        when(genAiSpeakingClient.evaluate(any())).thenReturn(new SpeakingEvaluationResponse(
            "Die Katze ist an den Tisch",
            6.5,
            false,
            "Check the preposition.",
            "grammar",
            "Die Katze ist auf dem Tisch",
            "UklGRg=="
        ));
        mockAnswerAndFeedbackSaves(21L, 22L, "2026-06-01T11:00:00Z");

        SubmitSpeakingAnswerResponse response = service.submitSpeakingAnswer(
            42L,
            7L,
            3L,
            1L,
            "German",
            "A2",
            audio()
        );

        assertThat(response.getAnswer().getAnswerText()).isEqualTo("Die Katze ist an den Tisch");
        assertThat(response.getAnswer().getScore()).isEqualTo(65);
        assertThat(response.getFeedback()).isNotNull();
        assertThat(response.getFeedback().getMessage()).isEqualTo("Check the preposition.");
        assertThat(response.getFeedback().getWeakArea()).isEqualTo("grammar");
        assertThat(response.getFeedback().getCorrectedAnswer()).isEqualTo("Die Katze ist auf dem Tisch");
        assertThat(response.getTranscription()).isEqualTo("Die Katze ist an den Tisch");
        assertThat(response.getFeedbackAudioB64()).isEqualTo("UklGRg==");
        verify(genAiSpeakingClient).evaluate(any());
        verify(progressRecordService).recordSubmittedAnswer(42L, 65);
    }

    @Test
    void submitSpeakingAnswerAllowsMissingFeedbackAudio() {
        UserAnswerService service = newService();

        whenSpeakingExercise();
        when(genAiSpeakingClient.evaluate(any())).thenReturn(new SpeakingEvaluationResponse(
            "Die Katze ist auf dem Tisch",
            8.0,
            true,
            "Clear and accurate.",
            "pronunciation",
            "Die Katze ist auf dem Tisch",
            null
        ));
        mockAnswerAndFeedbackSaves(23L, 24L, "2026-06-01T11:05:00Z");

        SubmitSpeakingAnswerResponse response = service.submitSpeakingAnswer(
            42L,
            7L,
            3L,
            1L,
            "German",
            "A2",
            audio()
        );

        assertThat(response.getAnswer().getAnswerText()).isEqualTo("Die Katze ist auf dem Tisch");
        assertThat(response.getAnswer().getScore()).isEqualTo(80);
        assertThat(response.getFeedback().getMessage()).isEqualTo("Clear and accurate.");
        assertThat(response.getFeedback().getWeakArea()).isEqualTo("pronunciation");
        assertThat(response.getFeedback().getCorrectedAnswer()).isEqualTo("Die Katze ist auf dem Tisch");
        assertThat(response.getFeedbackAudioB64()).isNull();
        verify(progressRecordService).recordSubmittedAnswer(42L, 80);
    }

    @Test
    void submitSpeakingAnswerClampsGenAiScoreToProgressBounds() {
        UserAnswerService service = newService();

        whenSpeakingExercise();
        when(genAiSpeakingClient.evaluate(any())).thenReturn(new SpeakingEvaluationResponse(
            "Die Katze ist auf dem Tisch",
            12.0,
            true,
            "Strong answer.",
            "none",
            "Die Katze ist auf dem Tisch",
            null
        ));
        mockAnswerAndFeedbackSaves(25L, 26L, "2026-06-01T11:10:00Z");

        SubmitSpeakingAnswerResponse response = service.submitSpeakingAnswer(
            42L,
            7L,
            3L,
            1L,
            "German",
            "A2",
            audio()
        );

        assertThat(response.getAnswer().getScore()).isEqualTo(100);
        assertThat(response.getLessonProgress()).isEqualTo(100);
        assertThat(response.getPlanProgress()).isEqualTo(100);
        verify(progressRecordService).recordSubmittedAnswer(42L, 100);
    }

    @Test
    void submitSpeakingAnswerRejectsMissingLessonId() {
        UserAnswerService service = newService();

        assertThatThrownBy(() -> service.submitSpeakingAnswer(42L, 7L, null, 1L, "German", "A2", audio()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(learningServiceClient, genAiSpeakingClient, userAnswerRepository, feedbackRepository);
        verify(progressRecordService, never()).recordSubmittedAnswer(any(), any(Integer.class));
    }

    @Test
    void submitSpeakingAnswerRejectsEmptyAudio() {
        UserAnswerService service = newService();
        MockMultipartFile emptyAudio = new MockMultipartFile("audio", "answer.webm", "audio/webm", new byte[0]);

        assertThatThrownBy(() -> service.submitSpeakingAnswer(42L, 7L, 3L, 1L, "German", "A2", emptyAudio))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(learningServiceClient, genAiSpeakingClient, userAnswerRepository, feedbackRepository);
        verify(progressRecordService, never()).recordSubmittedAnswer(any(), any(Integer.class));
    }

    @Test
    void submitSpeakingAnswerRejectsNonSpeakingExercise() {
        UserAnswerService service = newService();
        when(learningServiceClient.getExercise(3L, 7L)).thenReturn(new ExerciseContext(
            7L,
            "writing",
            "translation",
            "Translate: The cat is on the table",
            "Die Katze ist auf dem Tisch",
            "A2"
        ));

        assertThatThrownBy(() -> service.submitSpeakingAnswer(42L, 7L, 3L, 1L, "German", "A2", audio()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(learningServiceClient).getExercise(3L, 7L);
        verifyNoInteractions(genAiSpeakingClient, userAnswerRepository, feedbackRepository);
        verify(progressRecordService, never()).recordSubmittedAnswer(any(), any(Integer.class));
    }

    @Test
    void submitSpeakingAnswerPropagatesGenAiFailureWithoutPersistence() {
        UserAnswerService service = newService();
        whenSpeakingExercise();
        when(genAiSpeakingClient.evaluate(any())).thenThrow(
            new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GenAI speaking evaluation failed")
        );

        assertThatThrownBy(() -> service.submitSpeakingAnswer(42L, 7L, 3L, 1L, "German", "A2", audio()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_GATEWAY);

        verify(learningServiceClient).getExercise(3L, 7L);
        verify(genAiSpeakingClient).evaluate(any());
        verifyNoInteractions(userAnswerRepository, feedbackRepository);
        verify(progressRecordService, never()).recordSubmittedAnswer(any(), any(Integer.class));
    }

    @Test
    void submitSpeakingAnswerPropagatesMissingExercise() {
        UserAnswerService service = newService();
        when(learningServiceClient.getExercise(3L, 7L)).thenThrow(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise 7 not found in lesson 3")
        );

        assertThatThrownBy(() -> service.submitSpeakingAnswer(42L, 7L, 3L, 1L, "German", "A2", audio()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);

        verify(learningServiceClient).getExercise(3L, 7L);
        verifyNoInteractions(genAiSpeakingClient, userAnswerRepository, feedbackRepository);
        verify(progressRecordService, never()).recordSubmittedAnswer(any(), any(Integer.class));
    }

    @Test
    void submitSpeakingAnswerRejectsMismatchedAuthenticatedUser() {
        UserAnswerService service = newService();
        when(userServiceClient.resolveSubmittedUserId(42L)).thenThrow(
            new ResponseStatusException(HttpStatus.FORBIDDEN, "Submitted user_id does not match the authenticated user.")
        );

        assertThatThrownBy(() -> service.submitSpeakingAnswer(42L, 7L, 3L, 1L, "German", "A2", audio()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.FORBIDDEN);

        verifyNoInteractions(learningServiceClient, genAiSpeakingClient, userAnswerRepository, feedbackRepository);
        verify(progressRecordService, never()).recordSubmittedAnswer(any(), any(Integer.class));
    }

    private UserAnswerService newService() {
        lenient().when(userServiceClient.resolveSubmittedUserId(any())).thenAnswer(invocation -> invocation.getArgument(0));

        return new UserAnswerService(
            userAnswerRepository,
            feedbackRepository,
            progressRecordService,
            learningServiceClient,
            genAiWritingClient,
            genAiSpeakingClient,
            userServiceClient,
            listeningContentService,
            objectMapper
        );
    }

    private void whenSpeakingExercise() {
        when(learningServiceClient.getExercise(3L, 7L)).thenReturn(new ExerciseContext(
            7L,
            "speaking",
            "translation",
            "Translate: The cat is on the table",
            "Die Katze ist auf dem Tisch",
            "A2"
        ));
    }

    private void mockAnswerAndFeedbackSaves(Long answerId, Long feedbackId, String timestamp) {
        when(userAnswerRepository.save(any())).thenAnswer(invocation -> {
            UserAnswer answer = invocation.getArgument(0);
            answer.setId(answerId);
            answer.setSubmittedAt(Instant.parse(timestamp));
            return answer;
        });
        when(feedbackRepository.save(any())).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(feedbackId);
            feedback.setCreatedAt(Instant.parse(timestamp));
            return feedback;
        });
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("audio", "answer.webm", "audio/webm", "audio-bytes".getBytes());
    }
}
