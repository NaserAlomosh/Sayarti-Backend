package com.sayarti.backend.email.smtp;

import com.sayarti.backend.email.EmailDeliveryException;
import com.sayarti.backend.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);
    private static final String SUBJECT = "Verify your Sayarti email address";

    private final JavaMailSender mailSender;
    private final SmtpMailProperties properties;

    public SmtpEmailService(JavaMailSender mailSender, SmtpMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendVerificationOtp(String recipient, String otp, long expirationSeconds) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.from());
            helper.setTo(recipient);
            helper.setSubject(SUBJECT);
            helper.setText(plainText(otp, expirationSeconds), html(otp, expirationSeconds));
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            log.warn("SMTP verification delivery failed for recipient domain {}; category={}",
                    recipientDomain(recipient), exception.getClass().getSimpleName());
            throw new EmailDeliveryException();
        }
    }

    private String plainText(String otp, long expirationSeconds) {
        return "Sayarti email verification\n\nYour verification code is " + otp
                + ". It expires in " + duration(expirationSeconds) + ".\n\n"
                + "If you did not request this code, please ignore this email.";
    }

    private String html(String otp, long expirationSeconds) {
        return "<html><body><h1>Sayarti email verification</h1>"
                + "<p>Your verification code is:</p><p><strong style=\"font-size:24px\">"
                + otp + "</strong></p><p>This code expires in " + duration(expirationSeconds)
                + ".</p><p>If you did not request this code, please ignore this email.</p>"
                + "</body></html>";
    }

    private String duration(long expirationSeconds) {
        if (expirationSeconds % 60 == 0) {
            long minutes = expirationSeconds / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return expirationSeconds + (expirationSeconds == 1 ? " second" : " seconds");
    }

    private String recipientDomain(String recipient) {
        int separator = recipient.lastIndexOf('@');
        return separator >= 0 && separator < recipient.length() - 1
                ? recipient.substring(separator + 1) : "unknown";
    }
}
