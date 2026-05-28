package de.tum.aet.devops26.user_service.service;

import de.tum.aet.devops26.user_service.dto.CreateUserProfileRequest;
import de.tum.aet.devops26.user_service.dto.UserProfileResponse;
import de.tum.aet.devops26.user_service.model.UserProfile;
import de.tum.aet.devops26.user_service.repository.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserService userService;

    public UserProfile save(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    public List<UserProfile> findAll() {
        return userProfileRepository.findAll();
    }

    public Optional<UserProfile> findById(Long id) {
        return userProfileRepository.findById(id);
    }

    public Optional<UserProfile> findByUserId(Long userId) {
        return userProfileRepository.findByUserId(userId);
    }

    public void deleteById(Long id) {
        userProfileRepository.deleteById(id);
    }

    public Optional<UserProfileResponse> createUserProfile(Long userId, CreateUserProfileRequest request) {
        if (userService.findById(userId).isEmpty()) {
            return Optional.empty();
        }

        UserProfile userProfile = UserProfile.builder()
            .userId(userId)
            .targetLanguage(request.getTargetLanguage())
            .currentLevel(request.getCurrentLevel())
            .learningGoal(request.getLearningGoal())
            .build();

        return Optional.of(toResponse(save(userProfile)));
    }

    public Optional<UserProfileResponse> findResponseByUserId(Long userId) {
        return findByUserId(userId).map(this::toResponse);
    }

    private UserProfileResponse toResponse(UserProfile userProfile) {
        return new UserProfileResponse(
            userProfile.getId(),
            userProfile.getUserId(),
            userProfile.getTargetLanguage(),
            userProfile.getCurrentLevel(),
            userProfile.getLearningGoal()
        );
    }
}
