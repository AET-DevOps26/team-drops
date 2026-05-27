package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.LessonResponse;
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
    private final ExerciseService exerciseService;

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

    public Optional<LessonResponse> findResponseById(Long id) {
        return findById(id).map(this::toResponse);
    }

    public LessonResponse toResponse(Lesson lesson) {
        return new LessonResponse(
            lesson.getId(),
            lesson.getPlanId(),
            lesson.getTitle(),
            lesson.getTopic(),
            lesson.getOrderNumber(),
            exerciseService.findByLessonId(lesson.getId()).stream()
                .map(exerciseService::toResponse)
                .toList()
        );
    }
}
