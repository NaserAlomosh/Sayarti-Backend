package com.sayarti.backend.security.jwt;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String preferredLanguage) {
    public AuthenticatedUser(UUID id, String email) {
        this(id, email, "en");
    }
}
