package de.tum.aet.devops26.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.learning_service.dto.LessonResponse;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.model.LessonContentBlock;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import de.tum.aet.devops26.learning_service.repository.LessonContentBlockRepository;
import de.tum.aet.devops26.learning_service.repository.LessonRepository;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultExerciseTemplate;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanCatalog;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLearningPlanContent;
import de.tum.aet.devops26.learning_service.service.catalog.DefaultLessonTemplate;
import de.tum.aet.devops26.learning_service.service.catalog.LocalizedExercise;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @Mock
    private DefaultLearningPlanCatalog defaultLearningPlanCatalog;

    private LessonService service;

    @BeforeEach
    void setUp() {
        service = new LessonService(
            lessonRepository,
            exerciseService,
            lessonContentBlockRepository,
            learningPlanRepository,
            defaultLearningPlanCatalog
        );
    }

    @Test
    void saveContentBlocksNormalizesGeneratedStrings() {
        when(lessonContentBlockRepository.countByLessonId(7L)).thenReturn(0L);
        when(lessonContentBlockRepository.save(any(LessonContentBlock.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

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
    void saveContentBlocksReturnsEarlyForBlankContent() {
        List<LessonContentBlock> blocks = service.saveContentBlocks(7L, List.of(" ", "\n"));

        assertThat(blocks).isEmpty();
        verify(lessonContentBlockRepository, never()).countByLessonId(any());
        verify(lessonContentBlockRepository, never()).save(any(LessonContentBlock.class));
    }

    @Test
    void toResponseIncludesStoredContentBlocks() {
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
        when(exerciseService.toResponse(any(Exercise.class), any())).thenCallRealMethod();

        LessonResponse response = service.toResponse(lesson);

        assertThat(response.getContentBlocks()).hasSize(1);
        assertThat(response.getContentBlocks().getFirst().getType().getValue()).isEqualTo("content");
        assertThat(response.getContentBlocks().getFirst().getTitle()).isEqualTo("Lesson content");
        assertThat(response.getContentBlocks().getFirst().getText()).isEqualTo("Use situation, task, action, result.");
    }

    @Test
    void findResponseByIdReturnsGermanDefaultLessonExercisesAndKeywords() {
        Lesson lesson = Lesson.builder()
            .id(3L)
            .planId(7L)
            .title("Self Introduction")
            .topic("Introduce yourself professionally in an interview.")
            .orderNumber(1)
            .build();
        LearningPlan plan = LearningPlan.builder()
            .id(7L)
            .title("Job Interview Preparation")
            .build();
        List<Exercise> exercises = List.of(
            Exercise.builder()
                .id(11L)
                .lessonId(3L)
                .type("free_text")
                .question("Tell me about yourself.")
                .difficulty("A2")
                .expectedAnswer("Write a clear, professional answer using specific details and formal vocabulary.")
                .build(),
            Exercise.builder()
                .id(12L)
                .lessonId(3L)
                .type("free_text")
                .question("Write a short professional introduction.")
                .difficulty("A2")
                .expectedAnswer("Write a clear, professional answer using specific details and formal vocabulary.")
                .build()
        );
        DefaultLearningPlanContent germanTemplate = new DefaultLearningPlanContent(
            "Vorbereitung auf Vorstellungsgespräche",
            "Feste Lektionen zum Üben professioneller Antworten in Vorstellungsgesprächen.",
            "2 weeks",
            "Sich auf ein professionelles Vorstellungsgespräch vorbereiten",
            "German",
            "A2",
            "Verfasse eine klare, professionelle Antwort mit konkreten Details und formellem Wortschatz.",
            List.of(new DefaultLessonTemplate(
                "Selbstvorstellung",
                "Stelle dich in einem Vorstellungsgespräch professionell vor.",
                List.of(
                    new DefaultExerciseTemplate(
                        "Erzählen Sie mir etwas über sich.",
                        List.of("background", "motivation")
                    ),
                    new DefaultExerciseTemplate(
                        "Schreibe eine kurze professionelle Selbstvorstellung.",
                        List.of()
                    )
                )
            ))
        );

        when(lessonRepository.findById(3L)).thenReturn(Optional.of(lesson));
        when(learningPlanRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(defaultLearningPlanCatalog.findKeyByLocalizedTitle("Job Interview Preparation"))
            .thenReturn(Optional.of("job-interview"));
        when(defaultLearningPlanCatalog.findLocalizedByKey(any(), any())).thenReturn(germanTemplate);
        when(exerciseService.findByLessonId(3L)).thenReturn(exercises);
        when(exerciseService.toResponse(any(Exercise.class), any(LocalizedExercise.class))).thenCallRealMethod();
        when(lessonContentBlockRepository.findByLessonIdOrderByOrderNumberAsc(3L)).thenReturn(List.of());

        LessonResponse response = service.findResponseById(3L, "German").orElseThrow();

        assertThat(response.getTitle()).isEqualTo("Selbstvorstellung");
        assertThat(response.getTopic()).isEqualTo("Stelle dich in einem Vorstellungsgespräch professionell vor.");
        assertThat(response.getExercises()).extracting("question")
            .containsExactly(
                "Erzählen Sie mir etwas über sich.",
                "Schreibe eine kurze professionelle Selbstvorstellung."
            );
        assertThat(response.getExercises()).extracting("expectedAnswer")
            .containsOnly("Verfasse eine klare, professionelle Antwort mit konkreten Details und formellem Wortschatz.");
        assertThat(response.getExercises()).extracting("format")
            .containsOnly("Kurze schriftliche Antwort");
        assertThat(response.getExercises().get(0).getKeywords())
            .containsExactly("background", "motivation");
    }

    @Test
    void findResponseByIdReturnsGermanMachineLearningLessonExercisesAndKeywords() {
        Lesson lesson = Lesson.builder()
            .id(4L)
            .planId(8L)
            .title("Presenting an ML Project")
            .topic("Practice explaining an ML project.")
            .orderNumber(1)
            .build();
        LearningPlan plan = LearningPlan.builder()
            .id(8L)
            .title("Machine Learning Interview Track")
            .build();
        List<Exercise> exercises = List.of(Exercise.builder()
            .id(21L)
            .lessonId(4L)
            .type("free_text")
            .question("Describe a machine learning project you worked on.")
            .difficulty("Intermediate")
            .expectedAnswer("Clear technical reasoning, concrete examples, and structured explanations.")
            .build());
        DefaultLearningPlanContent germanTemplate = new DefaultLearningPlanContent(
            "Vorbereitung auf Machine-Learning-Interviews",
            "Feste Lektionen zum Üben von Antworten für Machine-Learning-Interviews auf Deutsch.",
            "4 weeks",
            "Sich gezielt auf Machine-Learning-Interviews auf Deutsch vorbereiten",
            "German",
            "Intermediate",
            "Klare technische Argumentation, konkrete Beispiele und strukturierte Erklärungen.",
            List.of(new DefaultLessonTemplate(
                "Ein ML-Projekt vorstellen",
                "Übe, ein ML-Projekt klar und überzeugend zu erklären.",
                List.of(new DefaultExerciseTemplate(
                    "Beschreibe ein Machine-Learning-Projekt, an dem du gearbeitet hast.",
                    List.of("Problem", "Datensatz", "Vorverarbeitung")
                ))
            ))
        );

        when(lessonRepository.findById(4L)).thenReturn(Optional.of(lesson));
        when(learningPlanRepository.findById(8L)).thenReturn(Optional.of(plan));
        when(defaultLearningPlanCatalog.findKeyByLocalizedTitle("Machine Learning Interview Track"))
            .thenReturn(Optional.of("machine-learning-interview"));
        when(defaultLearningPlanCatalog.findLocalizedByKey(any(), any())).thenReturn(germanTemplate);
        when(exerciseService.findByLessonId(4L)).thenReturn(exercises);
        when(exerciseService.toResponse(any(Exercise.class), any(LocalizedExercise.class))).thenCallRealMethod();
        when(lessonContentBlockRepository.findByLessonIdOrderByOrderNumberAsc(4L)).thenReturn(List.of());

        LessonResponse response = service.findResponseById(4L, "German").orElseThrow();

        assertThat(response.getTitle()).isEqualTo("Ein ML-Projekt vorstellen");
        assertThat(response.getTopic()).isEqualTo("Übe, ein ML-Projekt klar und überzeugend zu erklären.");
        assertThat(response.getExercises()).extracting("question")
            .containsExactly("Beschreibe ein Machine-Learning-Projekt, an dem du gearbeitet hast.");
        assertThat(response.getExercises().get(0).getExpectedAnswer())
            .isEqualTo("Klare technische Argumentation, konkrete Beispiele und strukturierte Erklärungen.");
        assertThat(response.getExercises().get(0).getKeywords())
            .containsExactly("Problem", "Datensatz", "Vorverarbeitung");
    }

    @Test
    void findResponseByIdReturnsLocalizedGermanSpeakingPromptAndExpectedAnswer() {
        Lesson lesson = Lesson.builder()
            .id(5L)
            .planId(9L)
            .title("Introduction and Motivation")
            .topic("Present your background.")
            .orderNumber(1)
            .build();
        LearningPlan plan = LearningPlan.builder()
            .id(9L)
            .title(LearningPlanSeeder.SPEAKING_TITLE)
            .build();
        Exercise exercise = Exercise.builder()
            .id(31L)
            .lessonId(5L)
            .type("speaking_prompt")
            .question("Tell me about yourself.")
            .difficulty("A2")
            .expectedAnswer("Summarize your background.")
            .build();
        DefaultLearningPlanContent germanTemplate = new DefaultLearningPlanContent(
            "Sprechtraining für Software-Engineering-Interviews",
            "Mündliches Training für typische Interviewfragen.",
            "1 Woche",
            "Mündliche Antworten verbessern",
            "German",
            "A2",
            "Gib eine klare mündliche Antwort.",
            List.of(new DefaultLessonTemplate(
                "Vorstellung und Motivation",
                "Präsentiere deinen Hintergrund.",
                List.of(new DefaultExerciseTemplate(
                    "Erzählen Sie mir etwas über sich.",
                    "speaking_prompt",
                    "Fasse deinen Hintergrund, deine Erfahrung und dein berufliches Ziel zusammen.",
                    List.of("Hintergrund", "Erfahrung", "berufliches Ziel")
                ))
            ))
        );

        when(lessonRepository.findById(5L)).thenReturn(Optional.of(lesson));
        when(learningPlanRepository.findById(9L)).thenReturn(Optional.of(plan));
        when(defaultLearningPlanCatalog.findKeyByLocalizedTitle(LearningPlanSeeder.SPEAKING_TITLE))
            .thenReturn(Optional.of(LearningPlanSeeder.SPEAKING_TEMPLATE_KEY));
        when(defaultLearningPlanCatalog.findLocalizedByKey(
            LearningPlanSeeder.SPEAKING_TEMPLATE_KEY,
            "German"
        )).thenReturn(germanTemplate);
        when(exerciseService.findByLessonId(5L)).thenReturn(List.of(exercise));
        when(exerciseService.toResponse(any(Exercise.class), any(LocalizedExercise.class))).thenCallRealMethod();
        when(lessonContentBlockRepository.findByLessonIdOrderByOrderNumberAsc(5L)).thenReturn(List.of());

        LessonResponse response = service.findResponseById(5L, "German").orElseThrow();

        assertThat(response.getTitle()).isEqualTo("Vorstellung und Motivation");
        assertThat(response.getExercises().get(0).getType().getValue()).isEqualTo("speaking");
        assertThat(response.getExercises().get(0).getSubtype().getValue()).isEqualTo("speaking_prompt");
        assertThat(response.getExercises().get(0).getQuestion()).isEqualTo("Erzählen Sie mir etwas über sich.");
        assertThat(response.getExercises().get(0).getExpectedAnswer())
            .isEqualTo("Fasse deinen Hintergrund, deine Erfahrung und dein berufliches Ziel zusammen.");
        assertThat(response.getExercises().get(0).getFormat()).isEqualTo("Gesprochene Antwort");
        assertThat(response.getExercises().get(0).getKeywords())
            .containsExactly("Hintergrund", "Erfahrung", "berufliches Ziel");
    }
}
