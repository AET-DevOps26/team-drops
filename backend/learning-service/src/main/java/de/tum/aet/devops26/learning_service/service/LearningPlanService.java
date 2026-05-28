package de.tum.aet.devops26.learning_service.service;

import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.LearningPlanResponse;
import de.tum.aet.devops26.learning_service.model.Exercise;
import de.tum.aet.devops26.learning_service.model.LearningPlan;
import de.tum.aet.devops26.learning_service.model.Lesson;
import de.tum.aet.devops26.learning_service.repository.LearningPlanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningPlanService {

    private static final String DEFAULT_TITLE = "Job Interview Preparation";
    private static final String DEFAULT_DESCRIPTION = "A default plan for practicing job interview conversations.";

    private final LearningPlanRepository learningPlanRepository;
    private final LessonService lessonService;
    private final ExerciseService exerciseService;

    @Transactional
    public LearningPlanResponse createDefaultLearningPlan(CreateDefaultLearningPlanRequest request) {
        LearningPlan plan = learningPlanRepository.save(LearningPlan.builder()
            .userId(request.getUserId())
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .build());

        Lesson introductionLesson = lessonService.save(Lesson.builder()
            .planId(plan.getId())
            .title("Introducing Yourself")
            .topic("Interview introduction")
            .orderNumber(1)
            .build());

        exerciseService.save(Exercise.builder()
            .lessonId(introductionLesson.getId())
            .type("free_text")
            .question("Tell me about yourself.")
            .difficulty("beginner")
            .expectedAnswer("A concise summary of experience, strengths, and motivation.")
            .build());

        Lesson behavioralLesson = lessonService.save(Lesson.builder()
            .planId(plan.getId())
            .title("Behavioral Questions")
            .topic("STAR answers")
            .orderNumber(2)
            .build());

        exerciseService.save(Exercise.builder()
            .lessonId(behavioralLesson.getId())
            .type("free_text")
            .question("Describe a challenging project and how you handled it.")
            .difficulty("intermediate")
            .expectedAnswer("A structured STAR response covering situation, task, action, and result.")
            .build());

        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<LearningPlanResponse> findResponsesByUserId(Long userId) {
        return learningPlanRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    private LearningPlanResponse toResponse(LearningPlan plan) {
        return new LearningPlanResponse(
            plan.getId(),
            plan.getUserId(),
            plan.getTitle(),
            plan.getDescription(),
            lessonService.findByPlanId(plan.getId()).stream()
                .map(lessonService::toResponse)
                .toList()
        );
    }
}
