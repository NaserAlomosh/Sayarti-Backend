package com.sayarti.backend.auth.repository;
import com.sayarti.backend.auth.entity.EmailVerificationOtp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationOtp> findFirstByUserIdAndVerifiedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(UUID userId);
    @Modifying
    @Query("update EmailVerificationOtp o set o.invalidatedAt=:now where o.user.id=:userId and o.verifiedAt is null and o.invalidatedAt is null")
    int invalidateActive(@Param("userId") UUID userId, @Param("now") Instant now);
}
