package com.sayarti.backend.email;
import org.springframework.stereotype.Service;
@Service
public class UnconfiguredEmailService implements EmailService {
    @Override
    public void sendVerificationOtp(String recipient, String otp, long expirationSeconds) {
        // Deliberately neither persists nor logs OTP plaintext while no provider is configured.
    }
}
