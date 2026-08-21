package com.sayarti.backend.auth.service;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("sayarti.email-otp")
public record EmailOtpProperties(long expirationSeconds, int maxAttempts, long resendCooldownSeconds) { }
