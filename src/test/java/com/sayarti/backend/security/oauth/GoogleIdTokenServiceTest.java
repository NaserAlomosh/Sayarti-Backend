package com.sayarti.backend.security.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class GoogleIdTokenServiceTest {
    private final GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
    private final GoogleIdTokenService service = new GoogleIdTokenService(verifier);

    @Test void acceptsOnlyIdentityFromVerifiedToken() throws Exception {
        GoogleIdToken token = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("subject-1").setEmail("verified@example.com").setEmailVerified(true);
        payload.set("given_name", "Verified"); payload.set("family_name", "Person");
        when(verifier.verify("valid")).thenReturn(token); when(token.getPayload()).thenReturn(payload);
        assertThat(service.verify("valid")).isEqualTo(new GoogleIdentity("subject-1", "verified@example.com", "Verified", "Person"));
    }

    @Test void rejectsInvalidExpiredAndWrongAudienceTokens() throws Exception {
        when(verifier.verify("invalid")).thenReturn(null);
        when(verifier.verify("expired")).thenReturn(null);
        when(verifier.verify("wrong-audience")).thenReturn(null);
        assertThatThrownBy(() -> service.verify("invalid")).isInstanceOf(GoogleAuthenticationException.class);
        assertThatThrownBy(() -> service.verify("expired")).isInstanceOf(GoogleAuthenticationException.class);
        assertThatThrownBy(() -> service.verify("wrong-audience")).isInstanceOf(GoogleAuthenticationException.class);
    }

    @Test void rejectsUnverifiedEmailAndVerificationIoFailure() throws Exception {
        GoogleIdToken token = mock(GoogleIdToken.class);
        when(token.getPayload()).thenReturn(new GoogleIdToken.Payload().setSubject("subject").setEmail("email@example.com").setEmailVerified(false));
        when(verifier.verify("unverified-email")).thenReturn(token);
        when(verifier.verify("network-failure")).thenThrow(new IOException("redacted"));
        assertThatThrownBy(() -> service.verify("unverified-email")).isInstanceOf(GoogleAuthenticationException.class);
        assertThatThrownBy(() -> service.verify("network-failure")).isInstanceOf(GoogleAuthenticationException.class);
    }
}
