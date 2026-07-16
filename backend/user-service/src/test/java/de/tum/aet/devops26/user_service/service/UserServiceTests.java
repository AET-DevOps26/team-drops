package de.tum.aet.devops26.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.user_service.dto.UserResponse;
import de.tum.aet.devops26.user_service.model.User;
import de.tum.aet.devops26.user_service.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Test
    void getOrCreateOidcUserCreatesLocalUserOnFirstLogin() {
        UserService userService = new UserService(userRepository);
        Jwt jwt = jwt("keycloak-subject", Map.of(
            "email", "ada@example.com",
            "name", "Ada Lovelace"
        ));
        when(userRepository.findByKeycloakSubject("keycloak-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserResponse response = userService.getOrCreateOidcUser(jwt);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getName()).isEqualTo("Ada Lovelace");
        assertThat(response.getEmail()).isEqualTo("ada@example.com");
        assertThat(response.getNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getKeycloakSubject()).isEqualTo("keycloak-subject");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("OIDC_USER_NO_LOCAL_PASSWORD");
    }

    @Test
    void getOrCreateOidcUserReturnsExistingUserByKeycloakSubject() {
        UserService userService = new UserService(userRepository);
        User existingUser = User.builder()
            .id(7L)
            .keycloakSubject("keycloak-subject")
            .name("Ada")
            .email("ada@example.com")
            .build();
        when(userRepository.findByKeycloakSubject("keycloak-subject")).thenReturn(Optional.of(existingUser));

        UserResponse response = userService.getOrCreateOidcUser(jwt("keycloak-subject", Map.of()));

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("ada@example.com");
        assertThat(response.getNewUser()).isFalse();
    }

    @Test
    void getOrCreateOidcUserLinksExistingUserByEmail() {
        UserService userService = new UserService(userRepository);
        Jwt jwt = jwt("keycloak-subject", Map.of(
            "email", "ada@example.com",
            "preferred_username", "ada"
        ));
        User existingUser = User.builder()
            .id(13L)
            .name("Old Name")
            .email("ada@example.com")
            .build();
        when(userRepository.findByKeycloakSubject("keycloak-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        UserResponse response = userService.getOrCreateOidcUser(jwt);

        assertThat(response.getId()).isEqualTo(13L);
        assertThat(response.getName()).isEqualTo("ada");
        assertThat(response.getNewUser()).isFalse();
        assertThat(existingUser.getKeycloakSubject()).isEqualTo("keycloak-subject");
        verify(userRepository).save(existingUser);
    }

    @Test
    void getOrCreateLocalDevUserCreatesStableFallbackUser() {
        UserService userService = new UserService(userRepository);
        when(userRepository.findByEmail("local-dev@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return user;
        });

        UserResponse response = userService.getOrCreateLocalDevUser();

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getName()).isEqualTo("Local Dev User");
        assertThat(response.getEmail()).isEqualTo("local-dev@example.com");
        assertThat(response.getNewUser()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getKeycloakSubject()).isNull();
        assertThat(userCaptor.getValue().getPasswordHash()).isNull();
    }

    @Test
    void getOrCreateLocalDevUserReturnsExistingFallbackUser() {
        UserService userService = new UserService(userRepository);
        User existingUser = User.builder()
            .id(100L)
            .name("Local Dev User")
            .email("local-dev@example.com")
            .build();
        when(userRepository.findByEmail("local-dev@example.com")).thenReturn(Optional.of(existingUser));

        UserResponse response = userService.getOrCreateLocalDevUser();

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getEmail()).isEqualTo("local-dev@example.com");
        assertThat(response.getNewUser()).isFalse();
    }

    private Jwt jwt(String subject, Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(subject)
            .claims(existingClaims -> existingClaims.putAll(claims))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }
}
