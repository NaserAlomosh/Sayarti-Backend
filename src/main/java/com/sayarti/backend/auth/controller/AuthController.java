package com.sayarti.backend.auth.controller;

import com.sayarti.backend.auth.dto.AuthResponse;
import com.sayarti.backend.auth.dto.GoogleLoginRequest;
import com.sayarti.backend.auth.dto.LoginRequest;
import com.sayarti.backend.auth.dto.LogoutResponse;
import com.sayarti.backend.auth.dto.RefreshRequest;
import com.sayarti.backend.auth.dto.RegisterRequest;
import com.sayarti.backend.auth.dto.RegistrationResponse;
import com.sayarti.backend.auth.dto.ResendVerificationRequest;
import com.sayarti.backend.auth.dto.ResendVerificationResponse;
import com.sayarti.backend.auth.dto.VerifyEmailRequest;
import com.sayarti.backend.auth.service.AuthService;
import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Local and Google account session lifecycle")
@SecurityRequirements
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a local user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Unverified local account created; verification email requested"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Email exists")
    })
    public ApiResponse<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(service.register(request), "Email verification required");
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify a local account email",
            description = "Consumes a six-digit, single-use OTP and creates the first session. "
                    + "Codes expire, are attempt-limited, and cannot be reused.")
    public ApiResponse<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(service.verifyEmail(request), "Email verified");
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend local email verification",
            description = "Subject to a server-side cooldown/rate limit. A replacement invalidates "
                    + "the previous OTP. Eligible and ineligible accounts receive the same response.")
    public ApiResponse<ResendVerificationResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        return ApiResponse.success(service.resendVerification(request),
                "If the account is eligible, a verification email has been requested");
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Session created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Invalid credentials"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "AUTH_EMAIL_NOT_VERIFIED: local email verification is required")
    })
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(service.login(request));
    }

    @PostMapping("/google")
    @Operation(
            summary = "Log in or register with Google",
            description =
                    "Verifies the mobile Google ID token server-side. Profile and email are "
                            + "accepted only from the verified token. Existing local accounts are "
                            + "not linked automatically.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Google session created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Invalid, expired, or wrong-audience Google ID token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description =
                        "Verified email belongs to a local account; authenticated linking is required")
    })
    public ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.success(service.google(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "New token pair issued"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Invalid, expired, revoked, or reused refresh token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "AUTH_EMAIL_NOT_VERIFIED: the local account is unverified")
    })
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(service.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Revoke a refresh token",
            description =
                    "Revokes the supplied refresh token. Already-issued stateless access tokens "
                            + "remain valid until expiration.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Refresh token revoked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Invalid or revoked refresh token")
    })
    public ApiResponse<LogoutResponse> logout(@Valid @RequestBody RefreshRequest request) {
        service.logout(request);

        return ApiResponse.success(new LogoutResponse(true), "Logout successful");
    }
}
