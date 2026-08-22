package com.sayarti.backend.notification.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@Conditional(FirebaseConfiguredCondition.class)
public class FirebaseConfiguration {
    @Bean
    FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        requireConfigured(properties.projectId(), "FIREBASE_PROJECT_ID");
        requireConfigured(properties.clientEmail(), "FIREBASE_CLIENT_EMAIL");
        requireConfigured(properties.privateKey(), "FIREBASE_PRIVATE_KEY");

        String serviceAccount = "{\"type\":\"service_account\",\"project_id\":\""
                + jsonEscape(properties.projectId()) + "\",\"client_email\":\""
                + jsonEscape(properties.clientEmail()) + "\",\"private_key\":\""
                + jsonEscape(properties.normalizedPrivateKey()) + "\"}";
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccount.getBytes(StandardCharsets.UTF_8)));
        FirebaseOptions options = FirebaseOptions.builder()
                .setProjectId(properties.projectId())
                .setCredentials(credentials)
                .build();
        return FirebaseApp.initializeApp(options, "sayarti");
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private static void requireConfigured(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " must be configured");
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
