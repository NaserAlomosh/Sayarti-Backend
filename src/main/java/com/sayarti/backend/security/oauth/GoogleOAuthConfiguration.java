package com.sayarti.backend.security.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class GoogleOAuthConfiguration {
    @Bean
    GoogleIdTokenVerifier googleIdTokenVerifier(GoogleOAuthProperties properties) {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID must be configured");
        }
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(properties.clientId()))
                .build();
    }
}
