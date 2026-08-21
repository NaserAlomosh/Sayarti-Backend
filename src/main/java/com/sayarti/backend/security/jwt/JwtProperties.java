package com.sayarti.backend.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sayarti.jwt")
public record JwtProperties(String accessSecret, long accessExpiration, long refreshExpiration) {
}
