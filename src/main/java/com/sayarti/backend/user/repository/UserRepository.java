package com.sayarti.backend.user.repository;

import com.sayarti.backend.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
    Optional<User> findByGoogleSubjectAndDeletedAtIsNull(String googleSubject);
    boolean existsByEmailIgnoreCase(String email);
}
