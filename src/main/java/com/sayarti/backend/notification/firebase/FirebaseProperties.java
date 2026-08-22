package com.sayarti.backend.notification.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sayarti.firebase")
public record FirebaseProperties(String projectId, String clientEmail, String privateKey) {
    String normalizedPrivateKey() {
        return privateKey.replace("\\n", "\n");
    }
}
