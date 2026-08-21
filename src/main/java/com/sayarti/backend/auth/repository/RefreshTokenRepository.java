package com.sayarti.backend.auth.repository;

import com.sayarti.backend.auth.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
