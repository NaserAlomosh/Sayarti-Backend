package com.sayarti.backend.auth.repository;
import com.sayarti.backend.auth.entity.RefreshToken;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,UUID> {
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 Optional<RefreshToken> findByTokenHash(String tokenHash);
}
