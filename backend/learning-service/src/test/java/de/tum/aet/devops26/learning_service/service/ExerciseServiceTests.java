package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.aet.devops26.learning_service.dto.ExerciseSubtype;
import de.tum.aet.devops26.learning_service.dto.ExerciseResponse;
import de.tum.aet.devops26.learning_service.dto.ExerciseType;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.repository.ExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTests {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Test
    void toResponseMapsSpeakingPromptMetadata() {
        ExerciseService service = new ExerciseService(exerciseRepository);
        Exercise exercise = Exercise.builder()
            .id(7L)
            .lessonId(3L)
            .type(ExerciseSubtype.SPEAKING_PROMPT.getValue())
            .question("Record a short introduction.")
            .difficulty("A2")
            .expectedAnswer("Mention your name, studies, and one professional goal.")
            .build();

        ExerciseResponse response = service.toResponse(exercise);

        assertThat(response.getType()).isEqualTo(ExerciseType.SPEAKING);
        assertThat(response.getSubtype()).isEqualTo(ExerciseSubtype.SPEAKING_PROMPT);
        assertThat(response.getQuestion()).isEqualTo("Record a short introduction.");
        assertThat(response.getTitle()).isEqualTo("Record a short introduction.");
        assertThat(response.getDifficulty()).isEqualTo("A2");
        assertThat(response.getExpectedAnswer()).isEqualTo("Mention your name, studies, and one professional goal.");
    }

    @Test
    void toResponseMapsListeningChoiceAsListening() {
        ExerciseService service = new ExerciseService(exerciseRepository);
        Exercise exercise = Exercise.builder()
            .id(8L)
            .lessonId(3L)
            .type(ExerciseSubtype.LISTENING_CHOICE.getValue())
            .question("Listen and choose the best answer.")
            .difficulty("A2")
            .expectedAnswer("Select the most accurate listening response.")
            .build();

        ExerciseResponse response = service.toResponse(exercise);

        assertThat(response.getType()).isEqualTo(ExerciseType.LISTENING);
        assertThat(response.getSubtype()).isEqualTo(ExerciseSubtype.LISTENING_CHOICE);
    }

    @Test
    void toResponseFallsBackToFreeTextWritingForUnknownOrNullSubtype() {
        ExerciseService service = new ExerciseService(exerciseRepository);

        ExerciseResponse unknownResponse = service.toResponse(Exercise.builder()
            .id(9L)
            .lessonId(3L)
            .type("unknown_subtype")
            .question("Write a response.")
            .difficulty("A2")
            .expectedAnswer("Use a complete sentence.")
            .build());
        ExerciseResponse nullResponse = service.toResponse(Exercise.builder()
            .id(10L)
            .lessonId(3L)
            .type(null)
            .question("Write another response.")
            .difficulty("A2")
            .expectedAnswer("Use a complete sentence.")
            .build());

        assertThat(unknownResponse.getType()).isEqualTo(ExerciseType.WRITING);
        assertThat(unknownResponse.getSubtype()).isEqualTo(ExerciseSubtype.FREE_TEXT);
        assertThat(nullResponse.getType()).isEqualTo(ExerciseType.WRITING);
        assertThat(nullResponse.getSubtype()).isEqualTo(ExerciseSubtype.FREE_TEXT);
    }
}
