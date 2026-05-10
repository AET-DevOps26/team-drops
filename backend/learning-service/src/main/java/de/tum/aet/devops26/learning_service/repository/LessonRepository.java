package de.tum.aet.devops26.learning_service.repository;

import de.tum.aet.devops26.learning_service.model.Lesson;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByPlanIdOrderByOrderNumberAsc(Long planId);
}
