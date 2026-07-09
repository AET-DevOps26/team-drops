package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.learning_service.dto.LessonResponse;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.model.LessonContentBlock;
import de.tum.aet.devops26.learning_service.repository.LessonContentBlockRepository;
import de.tum.aet.devops26.learning_service.repository.LessonRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonServiceTests {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ExerciseService exerciseService;

    @Mock
    private LessonContentBlockRepository lessonContentBlockRepository;

    @Test
    void saveContentBlocksNormalizesGeneratedStrings() {
        LessonService service = newService();
        when(lessonContentBlockRepository.findByLessonIdOrderByOrderNumberAsc(7L)).thenReturn(List.of());
        when(lessonContentBlockRepository.save(any(LessonContentBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonContentBlock> blocks = service.saveContentBlocks(7L, List.of(
            " Use situation, task, action, result. ",
            " ",
            "Keep answers concrete."
        ));

        assertThat(blocks).hasSize(2);
        assertThat(blocks).extracting(LessonContentBlock::getLessonId).containsOnly(7L);
        assertThat(blocks).extracting(LessonContentBlock::getOrderNumber).containsExactly(1, 2);
        assertThat(blocks).extracting(LessonContentBlock::getType).containsOnly("content");
        assertThat(blocks).extracting(LessonContentBlock::getText)
            .containsExactly("Use situation, task, action, result.", "Keep answers concrete.");
    }

    @Test
    void toResponseIncludesStoredContentBlocks() {
        LessonService service = newService();
        Lesson lesson = Lesson.builder()
            .id(7L)
            .planId(3L)
            .title("Interview Answers")
            .topic("STAR answers")
            .orderNumber(1)
            .build();
        when(exerciseService.findByLessonId(7L)).thenReturn(List.of(Exercise.builder()
            .id(9L)
            .lessonId(7L)
            .type("free_text")
            .question("Write a STAR answer.")
            .difficulty("B1")
            .expectedAnswer("A structured answer.")
            .build()));
        when(lessonContentBlockRepository.findByLessonIdOrderByOrderNumberAsc(7L)).thenReturn(List.of(
            LessonContentBlock.builder()
                .id(10L)
                .lessonId(7L)
                .orderNumber(1)
                .type("content")
                .title("Lesson content")
                .text("Use situation, task, action, result.")
                .build()
        ));
        when(exerciseService.toResponse(any(Exercise.class))).thenCallRealMethod();

        LessonResponse response = service.toResponse(lesson);

        assertThat(response.getContentBlocks()).hasSize(1);
        assertThat(response.getContentBlocks().getFirst().getType().getValue()).isEqualTo("content");
        assertThat(response.getContentBlocks().getFirst().getTitle()).isEqualTo("Lesson content");
        assertThat(response.getContentBlocks().getFirst().getText()).isEqualTo("Use situation, task, action, result.");
    }

    private LessonService newService() {
        return new LessonService(lessonRepository, exerciseService, lessonContentBlockRepository);
    }
}
