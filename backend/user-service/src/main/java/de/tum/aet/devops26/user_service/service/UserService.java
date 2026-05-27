package de.tum.aet.devops26.user_service.service;

import de.tum.aet.devops26.user_service.dto.CreateUserRequest;
import de.tum.aet.devops26.user_service.dto.UserResponse;
import de.tum.aet.devops26.user_service.model.User;
import de.tum.aet.devops26.user_service.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

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

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<UserResponse> createUser(CreateUserRequest request) {
        if (existsByEmail(request.getEmail())) {
            return Optional.empty();
        }

        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .passwordHash(hashPassword(request.getPassword()))
            .build();

        return Optional.of(toResponse(save(user)));
    }

    public Optional<UserResponse> findResponseById(Long id) {
        return findById(id).map(this::toResponse);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing is not available", exception);
        }
    }
}
