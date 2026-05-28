package de.tum.aet.devops26.user_service.api.impl;

import de.tum.aet.devops26.user_service.api.UserServiceApi;
import de.tum.aet.devops26.user_service.dto.CreateUserProfileRequest;
import de.tum.aet.devops26.user_service.dto.CreateUserRequest;
import de.tum.aet.devops26.user_service.dto.LoginRequest;
import de.tum.aet.devops26.user_service.dto.LoginResponse;
import de.tum.aet.devops26.user_service.dto.UpsertUserProfileRequest;
import de.tum.aet.devops26.user_service.dto.UserProfileResponse;
import de.tum.aet.devops26.user_service.dto.UserResponse;
import de.tum.aet.devops26.user_service.service.UserProfileService;
import de.tum.aet.devops26.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserServiceController implements UserServiceApi {

    private final UserService userService;
    private final UserProfileService userProfileService;

    @Override
    public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
        return userService.createUser(createUserRequest)
            .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user))
            .orElseGet(() -> ResponseEntity.status(409).build());
    }

    @Override
    public ResponseEntity<UserProfileResponse> createUserProfile(
        Long userId,
        CreateUserProfileRequest createUserProfileRequest
    ) {
        return userProfileService.createUserProfile(userId, createUserProfileRequest)
            .map(userProfile -> ResponseEntity.status(HttpStatus.CREATED).body(userProfile))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserResponse> getUserById(Long userId) {
        return userService.findResponseById(userId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<LoginResponse> loginUser(LoginRequest loginRequest) {
        return userService.loginUser(loginRequest)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserProfileResponse> getUserProfile(Long userId) {
        return userProfileService.findResponseByUserId(userId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserProfileResponse> upsertUserProfile(
        Long userId,
        UpsertUserProfileRequest upsertUserProfileRequest
    ) {
        return userProfileService.upsertUserProfile(userId, upsertUserProfileRequest)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
