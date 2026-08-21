package com.sayarti.backend.security.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sayarti.google")
public record GoogleOAuthProperties(String clientId) {}
