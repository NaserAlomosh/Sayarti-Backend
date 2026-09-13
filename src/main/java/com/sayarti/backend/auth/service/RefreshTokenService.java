package com.sayarti.backend.auth.service;

import com.sayarti.backend.auth.entity.RefreshToken;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.common.exception.ApiException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.security.jwt.JwtProperties;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.entity.AuthProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final SecureRandom random = new SecureRandom();
    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public IssuedToken issue(User user) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken entity = new RefreshToken(
                user, hash(raw), Instant.now().plusMillis(properties.refreshExpiration()));
        repository.save(entity);
        return new IssuedToken(raw, entity);
    }
    @Transactional
    public Rotation rotate(String raw) {
        RefreshToken old = lookup(raw);
        if (old.getRevokedAt() != null) {
            throw error(ErrorCode.AUTH_REFRESH_TOKEN_REUSED,
                    "Refresh token has already been used or revoked");
        }
        if (!old.getExpiresAt().isAfter(Instant.now())) {
            old.revoke(null);
            throw error(ErrorCode.AUTH_INVALID_REFRESH_TOKEN, "Refresh token has expired");
        }
        if (old.getUser().getAuthProvider() == AuthProvider.LOCAL
                && !old.getUser().isEmailVerified()) {
            throw new ApiException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED, HttpStatus.FORBIDDEN,
                    "Email verification is required");
        }
        IssuedToken next = issue(old.getUser());
        old.revoke(next.entity().getId());
        return new Rotation(old.getUser(), next.raw());
    }
    @Transactional
    public void revoke(String raw) {
        RefreshToken token = lookup(raw);
        if (token.getRevokedAt() != null) {
            throw error(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED, "Refresh token is already revoked");
        }
        token.revoke(null);
    }

    private RefreshToken lookup(String raw) {
        return repository.findByTokenHash(hash(raw)).orElseThrow(
                () -> error(ErrorCode.AUTH_INVALID_REFRESH_TOKEN, "Refresh token is invalid"));
    }

    private ApiException error(ErrorCode code, String msg) {
        return new ApiException(code, HttpStatus.UNAUTHORIZED, msg);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record IssuedToken(String raw, RefreshToken entity) {
    }
    public record Rotation(User user, String refreshToken) {
    }
}
