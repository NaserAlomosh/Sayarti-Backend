package com.sayarti.backend.security.jwt;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email) {
}
