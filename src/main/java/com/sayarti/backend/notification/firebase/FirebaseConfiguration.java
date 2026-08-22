package com.sayarti.backend.notification.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sayarti.backend.notification.NotificationProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@Conditional(FirebaseConfiguredCondition.class)
public class FirebaseConfiguration {
    static final String APP_NAME = "sayarti";

    @Bean
    FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        synchronized (FirebaseApp.class) {
            for (FirebaseApp existingApp : FirebaseApp.getApps()) {
                if (APP_NAME.equals(existingApp.getName())) {
                    return existingApp;
                }
            }

            GoogleCredentials credentials = loadCredentials(properties.serviceAccountPath());
            FirebaseOptions options = FirebaseOptions.builder()
                    .setProjectId(((ServiceAccountCredentials) credentials).getProjectId())
                    .setCredentials(credentials)
                    .build();
            return FirebaseApp.initializeApp(options, APP_NAME);
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @Bean
    NotificationProvider firebaseNotificationProvider(FirebaseMessaging firebaseMessaging) {
        return new FirebaseNotificationProvider(firebaseMessaging);
    }

    static GoogleCredentials loadCredentials(String serviceAccountPath) throws IOException {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            throw new IOException("Firebase service-account path is not configured");
        }
        Path path = Path.of(serviceAccountPath);
        try (InputStream stream = Files.newInputStream(path)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            if (!(credentials instanceof ServiceAccountCredentials)) {
                throw new IOException("Firebase credential file is not a service account");
            }
            return credentials;
        }
    }
}
