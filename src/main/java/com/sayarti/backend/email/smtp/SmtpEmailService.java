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
    private final JavaMailSender mailSender;
    private final SmtpMailProperties properties;
    private final com.sayarti.backend.i18n.MessageLocalizer localizer;

    @org.springframework.beans.factory.annotation.Autowired
    public SmtpEmailService(JavaMailSender mailSender, SmtpMailProperties properties,
            com.sayarti.backend.i18n.MessageLocalizer localizer) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.localizer = localizer;
    }
    public SmtpEmailService(JavaMailSender mailSender, SmtpMailProperties properties) {
        this(mailSender, properties, defaultLocalizer());
    }
    private static com.sayarti.backend.i18n.MessageLocalizer defaultLocalizer() {
        var source = new org.springframework.context.support.ResourceBundleMessageSource(); source.setBasename("messages");
        return new com.sayarti.backend.i18n.MessageLocalizer(source);
    }

    @Override
    public void sendVerificationOtp(String recipient, String otp, long expirationSeconds) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.from());
            helper.setTo(recipient);
            helper.setSubject(localizer.text("email.verification.subject", "Verify your Sayarti email address"));
            helper.setText(plainText(otp, expirationSeconds), html(otp, expirationSeconds));
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            log.warn("SMTP verification delivery failed for recipient domain {}; category={}",
                    recipientDomain(recipient), exception.getClass().getSimpleName());
            throw new EmailDeliveryException();
        }
    }

    private String plainText(String otp, long expirationSeconds) {
        return localizer.text("email.verification.heading", "Sayarti email verification") + "\n\n"
                + localizer.text("email.verification.code", "Your verification code is:") + " " + otp + "\n"
                + localizer.text("email.verification.expires", "This code expires in {0}.", duration(expirationSeconds)) + "\n\n"
                + localizer.text("email.verification.ignore", "If you did not request this code, please ignore this email.");
    }

    private String html(String otp, long expirationSeconds) {
        return "<html><body><h1>" + localizer.text("email.verification.heading", "Sayarti email verification") + "</h1>"
                + "<p>" + localizer.text("email.verification.code", "Your verification code is:") + "</p><p><strong style=\"font-size:24px\">"
                + otp + "</strong></p><p>" + localizer.text("email.verification.expires", "This code expires in {0}.", duration(expirationSeconds))
                + "</p><p>" + localizer.text("email.verification.ignore", "If you did not request this code, please ignore this email.") + "</p>"
                + "</body></html>";
    }

    private String duration(long expirationSeconds) {
        if (expirationSeconds % 60 == 0) {
            long minutes = expirationSeconds / 60;
            return localizer.text("time.minute", minutes + (minutes == 1 ? " minute" : " minutes"), minutes);
        }
        return localizer.text("time.second", expirationSeconds + (expirationSeconds == 1 ? " second" : " seconds"), expirationSeconds);
    }

    private String recipientDomain(String recipient) {
        int separator = recipient.lastIndexOf('@');
        return separator >= 0 && separator < recipient.length() - 1
                ? recipient.substring(separator + 1) : "unknown";
    }
}
