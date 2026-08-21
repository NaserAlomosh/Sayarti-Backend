package com.sayarti.backend.security.oauth;

public interface GoogleTokenVerifier {
    GoogleIdentity verify(String idToken);
}
