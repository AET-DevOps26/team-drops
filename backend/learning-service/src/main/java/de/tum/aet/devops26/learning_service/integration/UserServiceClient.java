package de.tum.aet.devops26.learning_service.integration;

import de.tum.aet.devops26.learning_service.security.CurrentBearerTokenResolver;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final CurrentBearerTokenResolver currentBearerTokenResolver;
    private final boolean authEnabled;

    public UserServiceClient(
        RestClient.Builder restClientBuilder,
        @Value("${services.user.base-url}") String baseUrl,
        CurrentBearerTokenResolver currentBearerTokenResolver,
        @Value("${app.auth.enabled:false}") boolean authEnabled
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.currentBearerTokenResolver = currentBearerTokenResolver;
        this.authEnabled = authEnabled;
    }

    public Long resolveSubmittedUserId(Long submittedUserId) {
        if (!authEnabled) {
            return submittedUserId;
        }

        Optional<String> bearerToken = currentBearerTokenResolver.resolveTokenValue();
        if (bearerToken.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated user lookup requires a Bearer token."
            );
        }

        try {
            RequestHeadersSpec<?> request = restClient.get().uri("/api/v1/users/me");
            request.header("Authorization", "Bearer " + bearerToken.get());

            UserContext currentUser = request.retrieve().body(UserContext.class);
            if (currentUser == null || currentUser.id() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User service returned no current user.");
            }

            if (submittedUserId != null && !currentUser.id().equals(submittedUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Submitted user_id does not match the authenticated user.");
            }

            return currentUser.id();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to validate current user.", exception);
        }
    }

    private record UserContext(Long id) {
    }
}
