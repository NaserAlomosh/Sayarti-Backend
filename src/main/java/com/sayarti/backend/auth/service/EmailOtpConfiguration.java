package com.sayarti.backend.auth.service;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Configuration
@EnableConfigurationProperties(EmailOtpProperties.class)
public class EmailOtpConfiguration { }
