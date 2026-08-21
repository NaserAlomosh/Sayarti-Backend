package com.sayarti.backend.auth.controller;
import com.sayarti.backend.auth.dto.*;
import com.sayarti.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth") @Tag(name="Authentication",description="Local and Google account session lifecycle") @SecurityRequirements
public class AuthController {
 private final AuthService service;public AuthController(AuthService service){this.service=service;}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Register a local user") @ApiResponses({@ApiResponse(responseCode="201",description="Account and session created"),@ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=com.sayarti.backend.common.response.ErrorResponse.class))),@ApiResponse(responseCode="409",description="Email exists")})
 public com.sayarti.backend.common.response.ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request){return com.sayarti.backend.common.response.ApiResponse.success(service.register(request),"Registration successful");}
 @PostMapping("/login") @Operation(summary="Log in with email and password") @ApiResponses({@ApiResponse(responseCode="200",description="Session created"),@ApiResponse(responseCode="401",description="Invalid credentials")})
 public com.sayarti.backend.common.response.ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request){return com.sayarti.backend.common.response.ApiResponse.success(service.login(request));}
 @PostMapping("/google") @Operation(summary="Log in or register with Google",description="Verifies the mobile Google ID token server-side. Profile and email are accepted only from the verified token. Existing local accounts are not linked automatically.") @ApiResponses({@ApiResponse(responseCode="200",description="Google session created"),@ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=com.sayarti.backend.common.response.ErrorResponse.class))),@ApiResponse(responseCode="401",description="Invalid, expired, or wrong-audience Google ID token"),@ApiResponse(responseCode="409",description="Verified email belongs to a local account; authenticated linking is required")})
 public com.sayarti.backend.common.response.ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request){return com.sayarti.backend.common.response.ApiResponse.success(service.google(request));}
 @PostMapping("/refresh") @Operation(summary="Rotate a refresh token") @ApiResponses({@ApiResponse(responseCode="200",description="New token pair issued"),@ApiResponse(responseCode="401",description="Invalid, expired, revoked, or reused refresh token")})
 public com.sayarti.backend.common.response.ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request){return com.sayarti.backend.common.response.ApiResponse.success(service.refresh(request));}
 @PostMapping("/logout") @Operation(summary="Revoke a refresh token",description="Revokes the supplied refresh token. Already-issued stateless access tokens remain valid until expiration.") @ApiResponses({@ApiResponse(responseCode="200",description="Refresh token revoked"),@ApiResponse(responseCode="401",description="Invalid or revoked refresh token")})
 public com.sayarti.backend.common.response.ApiResponse<LogoutResponse> logout(@Valid @RequestBody RefreshRequest request){service.logout(request);return com.sayarti.backend.common.response.ApiResponse.success(new LogoutResponse(true),"Logout successful");}
}
