package com.sayarti.backend.email.smtp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SmtpMailProperties.class)
public class SmtpMailConfiguration {
}
