package de.tum.aet.devops26.learning_service.api.impl;

import de.tum.aet.devops26.learning_service.api.LearningServiceApi;
import de.tum.aet.devops26.learning_service.dto.CreateDefaultLearningPlanRequest;
import de.tum.aet.devops26.learning_service.dto.LearningPlanResponse;
import de.tum.aet.devops26.learning_service.dto.LessonResponse;
import de.tum.aet.devops26.learning_service.service.LearningPlanService;
import de.tum.aet.devops26.learning_service.service.LessonService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LearningServiceController implements LearningServiceApi {

    private final LearningPlanService learningPlanService;
    private final LessonService lessonService;

    @Override
    public ResponseEntity<LearningPlanResponse> createDefaultLearningPlan(
        CreateDefaultLearningPlanRequest createDefaultLearningPlanRequest
    ) {
        return ResponseEntity.status(201)
            .body(learningPlanService.createDefaultLearningPlan(createDefaultLearningPlanRequest));
    }

    @Override
    public ResponseEntity<List<LearningPlanResponse>> getLearningPlansByUserId(Long userId) {
        List<LearningPlanResponse> learningPlans = learningPlanService.findResponsesByUserId(userId);
        if (learningPlans.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(learningPlans);
    }

    @Override
    public ResponseEntity<LessonResponse> getLessonById(Long lessonId) {
        return lessonService.findResponseById(lessonId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
