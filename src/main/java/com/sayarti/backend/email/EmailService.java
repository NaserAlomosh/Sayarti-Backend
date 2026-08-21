package com.sayarti.backend.email;

public interface EmailService {
    void sendVerificationOtp(String recipient, String otp, long expirationSeconds);
}
