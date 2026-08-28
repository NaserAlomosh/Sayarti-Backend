package com.sayarti.backend.auth.dto;

import com.sayarti.backend.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication result. When requiredAction is VERIFY_EMAIL, token fields "
        + "are null and the user does not have an authenticated session.")
public record AuthResponse(
        @Schema(nullable = true) String accessToken,
        @Schema(nullable = true) String refreshToken,
        @Schema(nullable = true) String tokenType,
        @Schema(nullable = true) Long expiresIn,
        UserResponse user,
        @Schema(nullable = true, allowableValues = {"VERIFY_EMAIL", "SELECT_COUNTRY"})
        String requiredAction) {
}
