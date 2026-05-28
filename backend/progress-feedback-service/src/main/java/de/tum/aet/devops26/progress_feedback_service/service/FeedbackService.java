package de.tum.aet.devops26.progress_feedback_service.service;

import de.tum.aet.devops26.progress_feedback_service.dto.FeedbackResponse;
import de.tum.aet.devops26.progress_feedback_service.model.Feedback;
import de.tum.aet.devops26.progress_feedback_service.repository.FeedbackRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public Feedback save(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }

    public Optional<Feedback> findById(Long id) {
        return feedbackRepository.findById(id);
    }

    public Optional<Feedback> findByAnswerId(Long answerId) {
        return feedbackRepository.findByAnswerId(answerId);
    }

    public Optional<FeedbackResponse> findResponseByAnswerId(Long answerId) {
        return findByAnswerId(answerId).map(this::toResponse);
    }

    public void deleteById(Long id) {
        feedbackRepository.deleteById(id);
    }

    private FeedbackResponse toResponse(Feedback feedback) {
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
