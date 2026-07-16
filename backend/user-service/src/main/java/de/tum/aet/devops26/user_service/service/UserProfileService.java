package de.tum.aet.devops26.user_service.service;

import de.tum.aet.devops26.user_service.dto.CreateUserProfileRequest;
import de.tum.aet.devops26.user_service.dto.UpsertUserProfileRequest;
import de.tum.aet.devops26.user_service.dto.UserProfileResponse;
import de.tum.aet.devops26.user_service.model.User;
import de.tum.aet.devops26.user_service.model.UserProfile;
import de.tum.aet.devops26.user_service.repository.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final String DEFAULT_COUNTRY = "Unknown";
    private static final String DEFAULT_TARGET_LANGUAGE = "German";
    private static final String DEFAULT_LEVEL = "A2";
    private static final String DEFAULT_LEARNING_GOAL =
        "Prepare for a software engineering job interview";

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
        return userService.findById(userId).map(user -> {
            UserProfile userProfile = UserProfile.builder()
                .userId(userId)
                .name(user.getName())
                .country(DEFAULT_COUNTRY)
                .targetLanguage(request.getTargetLanguage())
                .currentLevel(request.getCurrentLevel())
                .learningGoal(request.getLearningGoal())
                .build();

            return toResponse(save(userProfile));
        });
    }

    public Optional<UserProfileResponse> upsertUserProfile(Long userId, UpsertUserProfileRequest request) {
        return userService.findById(userId).map(user -> {
            UserProfile userProfile = findByUserId(userId).orElseGet(UserProfile::new);
            userProfile.setUserId(userId);
            userProfile.setName(request.getName());
            userProfile.setCountry(request.getCountry());
            userProfile.setTargetLanguage(request.getTargetLanguage());
            userProfile.setCurrentLevel(request.getCurrentLevel());
            userProfile.setLearningGoal(request.getLearningGoal());

            syncUserName(user, request.getName());
            return toResponse(save(userProfile));
        });
    }

    public Optional<UserProfileResponse> findResponseByUserId(Long userId) {
        return findByUserId(userId).map(this::toResponse);
    }

    public Optional<UserProfileResponse> findOrCreateResponseByUserId(Long userId) {
        return userService.findById(userId).map(user -> {
            UserProfile profile = findByUserId(userId).orElseGet(() -> save(UserProfile.builder()
                .userId(userId)
                .name(user.getName())
                .country(DEFAULT_COUNTRY)
                .targetLanguage(DEFAULT_TARGET_LANGUAGE)
                .currentLevel(DEFAULT_LEVEL)
                .learningGoal(DEFAULT_LEARNING_GOAL)
                .build()));
            return toResponse(profile);
        });
    }

    private UserProfileResponse toResponse(UserProfile userProfile) {
        return new UserProfileResponse(
            userProfile.getId(),
            userProfile.getUserId(),
            userProfile.getName(),
            userProfile.getCountry(),
            userProfile.getTargetLanguage(),
            userProfile.getCurrentLevel(),
            userProfile.getLearningGoal()
        );
    }

    private void syncUserName(User user, String name) {
        if (!name.equals(user.getName())) {
            user.setName(name);
            userService.save(user);
        }
    }
}
