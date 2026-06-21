package de.tum.aet.devops26.progress_feedback_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.aet.devops26.progress_feedback_service.integration.GenAiListeningClient.ListeningGenerateResponse;
import de.tum.aet.devops26.progress_feedback_service.model.ListeningContent;
import de.tum.aet.devops26.progress_feedback_service.repository.ListeningContentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ListeningCacheWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListeningCacheWriter.class);

    private final ListeningContentRepository listeningContentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Persists listening content in its own REQUIRES_NEW transaction so that a
     * DataIntegrityViolationException (concurrent insert on the unique exercise_id)
     * rolls back only this inner transaction and does not poison any outer transaction.
     *
     * @return true if saved, false if a concurrent request already persisted the same exercise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean trySave(Long exerciseId, String scriptText, ListeningGenerateResponse response) {
        try {
            String questionsJson = objectMapper.writeValueAsString(
                    new ListeningGenerateResponse(response.script(), response.questions(), null));
            listeningContentRepository.save(ListeningContent.builder()
                    .exerciseId(exerciseId)
                    .scriptText(scriptText)
                    .questionsJson(questionsJson)
                    .scriptAudioB64(response.scriptAudioB64())
                    .build());
            return true;
        } catch (DataIntegrityViolationException e) {
            LOGGER.info("Listening content for exercise {} already cached (concurrent creation)", exerciseId);
            return false;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize listening content for persistence.", e);
        }
    }
}
