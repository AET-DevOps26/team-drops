package de.tum.aet.devops26.progress_feedback_service.service;

import de.tum.aet.devops26.progress_feedback_service.model.UserAnswer;
import de.tum.aet.devops26.progress_feedback_service.repository.UserAnswerRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAnswerService {

    private final UserAnswerRepository userAnswerRepository;

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
}
