package com.sayarti.backend.auth.entity;

import com.sayarti.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_otps")
public class EmailVerificationOtp {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "otp_hash", nullable = false, length = 100) private String otpHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "invalidated_at") private Instant invalidatedAt;
    protected EmailVerificationOtp() { }
    public EmailVerificationOtp(User user, String otpHash, Instant expiresAt, Instant createdAt) {
        this.id = UUID.randomUUID(); this.user = user; this.otpHash = otpHash;
        this.expiresAt = expiresAt; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getOtpHash() { return otpHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getInvalidatedAt() { return invalidatedAt; }
    public void failAttempt() { attemptCount++; }
    public void verify(Instant now) { verifiedAt = now; }
    public void invalidate(Instant now) { invalidatedAt = now; }
}
