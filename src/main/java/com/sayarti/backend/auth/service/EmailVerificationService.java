package com.sayarti.backend.auth.service;

import com.sayarti.backend.auth.entity.EmailVerificationOtp;
import com.sayarti.backend.auth.repository.EmailVerificationOtpRepository;
import com.sayarti.backend.common.exception.ApiException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.email.EmailService;
import com.sayarti.backend.user.entity.User;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {
    private final EmailVerificationOtpRepository repository;
    private final OtpSecurityService security;
    private final EmailService emailService;
    private final EmailOtpProperties properties;

    public EmailVerificationService(EmailVerificationOtpRepository repository,
            OtpSecurityService security, EmailService emailService, EmailOtpProperties properties) {
        this.repository = repository;
        this.security = security;
        this.emailService = emailService;
        this.properties = properties;
    }

    public void start(User user) {
        Instant now = Instant.now();
        repository.invalidateActive(user.getId(), now);
        String plaintext = security.generate();
        repository.saveAndFlush(new EmailVerificationOtp(user, security.hash(plaintext),
                now.plusSeconds(properties.expirationSeconds()), now));
        emailService.sendVerificationOtp(user.getEmail(), plaintext, properties.expirationSeconds());
    }

    public void verify(User user, String submittedOtp) {
        EmailVerificationOtp otp = repository
                .findFirstByUserIdAndVerifiedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                        user.getId())
                .orElseThrow(() -> error(ErrorCode.AUTH_OTP_INVALID, HttpStatus.BAD_REQUEST,
                        "Verification code is invalid"));
        Instant now = Instant.now();
        if (!otp.getExpiresAt().isAfter(now)) {
            otp.invalidate(now);
            throw error(ErrorCode.AUTH_OTP_EXPIRED, HttpStatus.BAD_REQUEST,
                    "Verification code has expired");
        }
        if (otp.getAttemptCount() >= properties.maxAttempts()) {
            otp.invalidate(now);
            throw attemptsExceeded();
        }
        if (!security.matches(submittedOtp, otp.getOtpHash())) {
            otp.failAttempt();
            if (otp.getAttemptCount() >= properties.maxAttempts()) {
                otp.invalidate(now);
                throw attemptsExceeded();
            }
            throw error(ErrorCode.AUTH_OTP_INVALID, HttpStatus.BAD_REQUEST,
                    "Verification code is invalid");
        }
        otp.verify(now);
        user.verifyEmail();
    }

    public void resend(User user) {
        Instant now = Instant.now();
        repository.findFirstByUserIdAndVerifiedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                        user.getId())
                .ifPresent(active -> {
                    if (active.getCreatedAt().plusSeconds(properties.resendCooldownSeconds())
                            .isAfter(now)) {
                        throw error(ErrorCode.AUTH_OTP_RESEND_TOO_SOON, HttpStatus.TOO_MANY_REQUESTS,
                                "Verification code was requested too recently");
                    }
                });
        start(user);
    }

    private ApiException attemptsExceeded() {
        return error(ErrorCode.AUTH_OTP_ATTEMPTS_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS,
                "Verification attempts exceeded");
    }

    private ApiException error(ErrorCode code, HttpStatus status, String message) {
        return new ApiException(code, status, message);
    }
}
