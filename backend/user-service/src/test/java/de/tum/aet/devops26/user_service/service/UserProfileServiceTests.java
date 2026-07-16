package de.tum.aet.devops26.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.aet.devops26.user_service.model.User;
import de.tum.aet.devops26.user_service.model.UserProfile;
import de.tum.aet.devops26.user_service.repository.UserProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTests {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserService userService;

    @Test
    void findOrCreateResponseCreatesDefaultsForNewUser() {
        UserProfileService service = new UserProfileService(userProfileRepository, userService);
        User user = User.builder().id(52L).name("Ada").email("ada@example.com").build();
        when(userService.findById(52L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(52L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile profile = invocation.getArgument(0);
            profile.setId(7L);
            return profile;
        });

        var response = service.findOrCreateResponseByUserId(52L);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().getId()).isEqualTo(7L);
        assertThat(response.orElseThrow().getName()).isEqualTo("Ada");
        assertThat(response.orElseThrow().getCountry()).isEqualTo("Unknown");
        assertThat(response.orElseThrow().getTargetLanguage()).isEqualTo("German");
        assertThat(response.orElseThrow().getCurrentLevel()).isEqualTo("A2");
        assertThat(response.orElseThrow().getLearningGoal())
            .isEqualTo("Prepare for a software engineering job interview");
    }

    @Test
    void findOrCreateResponseReturnsExistingProfile() {
        UserProfileService service = new UserProfileService(userProfileRepository, userService);
        User user = User.builder().id(52L).name("Ada").email("ada@example.com").build();
        UserProfile profile = UserProfile.builder()
            .id(8L)
            .userId(52L)
            .name("Ada")
            .country("Germany")
            .targetLanguage("English")
            .currentLevel("B2")
            .build();
        when(userService.findById(52L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(52L)).thenReturn(Optional.of(profile));

        var response = service.findOrCreateResponseByUserId(52L);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().getTargetLanguage()).isEqualTo("English");
        verify(userProfileRepository, never()).save(any());
    }
}
