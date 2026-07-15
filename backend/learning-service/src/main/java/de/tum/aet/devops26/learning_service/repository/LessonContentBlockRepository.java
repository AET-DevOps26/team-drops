package de.tum.aet.devops26.learning_service.repository;

import de.tum.aet.devops26.learning_service.model.LessonContentBlock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonContentBlockRepository extends JpaRepository<LessonContentBlock, Long> {

    List<LessonContentBlock> findByLessonIdOrderByOrderNumberAsc(Long lessonId);
}
