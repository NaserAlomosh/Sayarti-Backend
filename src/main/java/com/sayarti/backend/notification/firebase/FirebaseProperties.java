package com.sayarti.backend.notification.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sayarti.firebase")
public record FirebaseProperties(String serviceAccountPath) {
}
