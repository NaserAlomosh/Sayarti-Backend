package com.sayarti.backend.auth.repository;

import com.sayarti.backend.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken token
            set token.revokedAt = :revokedAt
            where token.user.id = :userId and token.revokedAt is null
            """)
    int revokeAllActiveByUserId(
            @Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
