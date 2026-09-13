package com.sayarti.backend.email.smtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.email.EmailDeliveryException;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailServiceTest {
    private JavaMailSender mailSender;
    private MimeMessage message;
    private SmtpEmailService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        service = new SmtpEmailService(mailSender,
                new SmtpMailProperties("no-reply@sayarti.example"));
    }

    @Test
    void buildsAndSendsVerificationMessage() throws Exception {
        service.sendVerificationOtp("driver@example.com", "123456", 300);

        verify(mailSender).send(message);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
                .isEqualTo("driver@example.com");
        assertThat(message.getFrom()[0].toString()).isEqualTo("no-reply@sayarti.example");
        assertThat(message.getSubject()).isEqualTo("Verify your Sayarti email address");
        assertThat(messageContent(message)).contains("123456", "5 minutes", "Sayarti");
    }

    @Test
    void convertsMailFailureToSafeApplicationException() {
        when(mailSender.createMimeMessage()).thenThrow(new MailSendException("SMTP detail"));

        assertThatThrownBy(() -> service.sendVerificationOtp(
                "driver@example.com", "123456", 300))
                .isInstanceOf(EmailDeliveryException.class)
                .satisfies(exception -> assertThat(((EmailDeliveryException) exception)
                        .getErrorCode()).isEqualTo(ErrorCode.EMAIL_DELIVERY_FAILED))
                .hasMessageNotContaining("SMTP detail")
                .hasMessageNotContaining("123456");
    }

    private String messageContent(MimeMessage mimeMessage) throws Exception {
        return contentText(mimeMessage.getContent());
    }

    private String contentText(Object content) throws Exception {
        if (content instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                text.append(contentText(multipart.getBodyPart(index).getContent()));
            }
            return text.toString();
        }
        return content.toString();
    }
}
