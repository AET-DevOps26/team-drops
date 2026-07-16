package de.tum.aet.devops26.user_service.service;

import de.tum.aet.devops26.user_service.dto.UserResponse;
import de.tum.aet.devops26.user_service.model.User;
import de.tum.aet.devops26.user_service.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String OIDC_PASSWORD_HASH_PLACEHOLDER = "OIDC_USER_NO_LOCAL_PASSWORD";
    private static final String LOCAL_DEV_EMAIL = "local-dev@example.com";
    private static final String LOCAL_DEV_NAME = "Local Dev User";

    private final UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<UserResponse> findResponseById(Long id) {
        return findById(id).map(user -> toResponse(user, false));
    }

    public UserResponse getOrCreateOidcUser(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = claimAsString(jwt, "email")
            .orElseGet(() -> subject + "@keycloak.local");
        String name = claimAsString(jwt, "name")
            .or(() -> claimAsString(jwt, "preferred_username"))
            .orElse(email);

        Optional<User> existingBySubject = userRepository.findByKeycloakSubject(subject);
        if (existingBySubject.isPresent()) {
            return toResponse(existingBySubject.orElseThrow(), false);
        }

        Optional<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User existingUser = existingByEmail.orElseThrow();
            existingUser.setKeycloakSubject(subject);
            existingUser.setName(name);
            return toResponse(save(existingUser), false);
        }

        return toResponse(save(User.builder()
            .keycloakSubject(subject)
            .name(name)
            .email(email)
            .passwordHash(OIDC_PASSWORD_HASH_PLACEHOLDER)
            .build()), true);
    }

    public UserResponse getOrCreateLocalDevUser() {
        return userRepository.findByEmail(LOCAL_DEV_EMAIL)
            .map(user -> toResponse(user, false))
            .orElseGet(() -> toResponse(save(User.builder()
                .name(LOCAL_DEV_NAME)
                .email(LOCAL_DEV_EMAIL)
                .build()), true));
    }

    private UserResponse toResponse(User user, boolean newUser) {
        UserResponse response = new UserResponse(user.getId(), user.getName(), user.getEmail());
        response.setNewUser(newUser);
        return response;
    }

    private Optional<String> claimAsString(Jwt jwt, String claimName) {
        Map<String, Object> claims = jwt.getClaims();
        Object value = claims.get(claimName);
        return value instanceof String stringValue && !stringValue.isBlank()
            ? Optional.of(stringValue)
            : Optional.empty();
    }
}
