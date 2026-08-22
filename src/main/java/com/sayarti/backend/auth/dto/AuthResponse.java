package com.sayarti.backend.auth.dto;

import com.sayarti.backend.user.dto.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, String tokenType,
        long expiresIn, UserResponse user, String requiredAction) {
}
