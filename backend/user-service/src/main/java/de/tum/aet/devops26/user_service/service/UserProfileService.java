package de.tum.aet.devops26.user_service.service;

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
}
