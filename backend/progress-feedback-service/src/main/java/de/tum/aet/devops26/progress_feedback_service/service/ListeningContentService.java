package de.tum.aet.devops26.progress_feedback_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiListeningClient;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiListeningClient.ListeningGenerateRequest;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiListeningClient.ListeningGenerateResponse;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiListeningClient.ListeningQuestion;
import de.tum.aet.devops26.progress_feedback_service.repository.ListeningContentRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ListeningContentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListeningContentService.class);

    private final ListeningContentRepository listeningContentRepository;
    private final GenAiListeningClient genAiListeningClient;
    private final ListeningCacheWriter listeningCacheWriter;
    private final ObjectMapper objectMapper;

    public record ScoreResult(int score, int correct, int total) {}

    /** Returns cached content if present, otherwise generates, caches, and returns fresh content. */
    public ListeningGenerateResponse generateOrLoad(Long exerciseId, String language, String level, String topic) {
        return listeningContentRepository.findByExerciseId(exerciseId)
                .map(cached -> {
                    LOGGER.info("Returning cached listening content for exercise {}", exerciseId);
                    return deserializeQuestions(cached.getQuestionsJson());
                })
                .orElseGet(() -> generateFresh(exerciseId, language, level, topic));
    }

    /**
     * Generates and caches content without checking the cache first.
     * Use this when the caller has already confirmed there is no cached entry.
     */
    public ListeningGenerateResponse generateFresh(Long exerciseId, String language, String level, String topic) {
        ListeningGenerateResponse response = genAiListeningClient.generate(
                new ListeningGenerateRequest(language, level, topic));

        boolean saved = listeningCacheWriter.trySave(exerciseId, response.script(), response);
        if (!saved) {
            // A concurrent request won the race; load what it saved
            return listeningContentRepository.findByExerciseId(exerciseId)
                    .map(cached -> deserializeQuestions(cached.getQuestionsJson()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Concurrent cache write failed and fallback read missed."));
        }
        return response;
    }

    public ScoreResult scoreAnswers(Long exerciseId, Map<Integer, String> selectedAnswers) {
        String questionsJson = listeningContentRepository.findByExerciseId(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No listening content found for exercise " + exerciseId + ". Call generate first."))
                .getQuestionsJson();

        List<ListeningQuestion> questions = deserializeQuestions(questionsJson).questions();
        if (questions == null || questions.isEmpty()) {
            return new ScoreResult(0, 0, 0);
        }

        int total = questions.size();
        long correct = 0;
        for (int i = 0; i < total; i++) {
            String selected = selectedAnswers.get(i);
            if (selected == null) {
                continue;
            }
            boolean isCorrect = questions.get(i).options().stream()
                    .anyMatch(opt -> opt.isCorrect() && opt.text().trim().equalsIgnoreCase(selected.trim()));
            if (isCorrect) {
                correct++;
            }
        }

        return new ScoreResult(
                (int) Math.round((double) correct / total * 100),
                (int) correct,
                total);
    }

    private ListeningGenerateResponse deserializeQuestions(String questionsJson) {
        try {
            ListeningGenerateResponse cached = objectMapper.readValue(questionsJson, ListeningGenerateResponse.class);
            return new ListeningGenerateResponse(cached.script(), cached.questions(), null);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to deserialize cached listening content.", exception);
        }
    }
}
