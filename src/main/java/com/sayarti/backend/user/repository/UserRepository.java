package com.sayarti.backend.user.repository;
import com.sayarti.backend.user.entity.User;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByEmailIgnoreCase(String email);
}
