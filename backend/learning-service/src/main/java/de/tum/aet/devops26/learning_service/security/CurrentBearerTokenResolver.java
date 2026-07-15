package de.tum.aet.devops26.learning_service.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CurrentBearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    public Optional<String> resolveTokenValue() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Optional.of(jwtAuthentication.getToken().getTokenValue());
        }

        return currentRequest()
            .map(request -> request.getHeader("Authorization"))
            .filter(header -> header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()))
            .map(header -> header.substring(BEARER_PREFIX.length()).trim())
            .filter(token -> !token.isBlank());
    }

    private Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.of(attributes.getRequest());
        }
        return Optional.empty();
    }
}
