package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LessonRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;

    public Lesson save(Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    public List<Lesson> findAll() {
        return lessonRepository.findAll();
    }

    public Optional<Lesson> findById(Long id) {
        return lessonRepository.findById(id);
    }

    public List<Lesson> findByPlanId(Long planId) {
        return lessonRepository.findByPlanIdOrderByOrderNumberAsc(planId);
    }

    public void deleteById(Long id) {
        lessonRepository.deleteById(id);
    }
}
