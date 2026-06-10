package de.tum.aet.devops26.user_service.repository;

import de.tum.aet.devops26.user_service.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakSubject(String keycloakSubject);
}
