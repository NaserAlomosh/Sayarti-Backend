package com.sayarti.backend.security.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.stereotype.Service;

@Service
public class GoogleIdTokenService implements GoogleTokenVerifier {
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenService(GoogleIdTokenVerifier verifier) { this.verifier = verifier; }

    @Override
    public GoogleIdentity verify(String rawToken) {
        try {
            GoogleIdToken token = verifier.verify(rawToken);
            if (token == null) throw invalid();
            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified()) || blank(payload.getSubject()) || blank(payload.getEmail())) {
                throw invalid();
            }
            return new GoogleIdentity(payload.getSubject(), payload.getEmail(),
                    stringClaim(payload, "given_name"), stringClaim(payload, "family_name"));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private String stringClaim(GoogleIdToken.Payload payload, String name) {
        Object value = payload.get(name);
        return value instanceof String text ? text : "";
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private GoogleAuthenticationException invalid() {
        return new GoogleAuthenticationException("Google ID token is invalid or expired");
    }
}
