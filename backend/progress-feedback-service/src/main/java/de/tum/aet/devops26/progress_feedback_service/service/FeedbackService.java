package de.tum.aet.devops26.progress_feedback_service.service;

import de.tum.aet.devops26.progress_feedback_service.model.Feedback;
import de.tum.aet.devops26.progress_feedback_service.repository.FeedbackRepository;
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

    public void deleteById(Long id) {
        feedbackRepository.deleteById(id);
    }
}
