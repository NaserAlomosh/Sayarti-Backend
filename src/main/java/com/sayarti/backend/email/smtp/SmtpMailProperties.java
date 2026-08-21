package com.sayarti.backend.email.smtp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sayarti.email")
public record SmtpMailProperties(String from) {
}
