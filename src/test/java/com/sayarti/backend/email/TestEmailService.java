package com.sayarti.backend.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** In-memory email adapter for tests. It never delegates to an external mail server. */
public class TestEmailService implements EmailService {
    private final List<VerificationEmail> verificationEmails = new ArrayList<>();

    @Override
    public synchronized void sendVerificationOtp(
            String recipient, String otp, long expirationSeconds) {
        verificationEmails.add(new VerificationEmail(recipient, otp, expirationSeconds));
    }

    public synchronized String latestOtpFor(String recipient) {
        String normalizedRecipient = recipient.toLowerCase(Locale.ROOT);
        return verificationEmails.stream()
                .filter(email -> email.recipient().toLowerCase(Locale.ROOT)
                        .equals(normalizedRecipient))
                .reduce((first, second) -> second)
                .map(VerificationEmail::otp)
                .orElseThrow(() -> new IllegalStateException(
                        "No verification email captured for " + recipient));
    }

    public synchronized List<VerificationEmail> sentVerificationEmails() {
        return List.copyOf(verificationEmails);
    }

    public synchronized void clear() {
        verificationEmails.clear();
    }

    public record VerificationEmail(String recipient, String otp, long expirationSeconds) {
    }
}
