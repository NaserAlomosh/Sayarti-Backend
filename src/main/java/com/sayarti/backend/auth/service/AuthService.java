package com.sayarti.backend.auth.service;

import com.sayarti.backend.auth.dto.AuthResponse;
import com.sayarti.backend.auth.dto.GoogleLoginRequest;
import com.sayarti.backend.auth.dto.LoginRequest;
import com.sayarti.backend.auth.dto.RefreshRequest;
import com.sayarti.backend.auth.dto.RegisterRequest;
import com.sayarti.backend.common.exception.ApiException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.security.jwt.JwtService;
import com.sayarti.backend.security.oauth.GoogleIdentity;
import com.sayarti.backend.security.oauth.GoogleTokenVerifier;
import com.sayarti.backend.user.dto.UserResponse;
import com.sayarti.backend.user.entity.AuthProvider;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final GoogleTokenVerifier googleTokens;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
            RefreshTokenService refreshTokens, GoogleTokenVerifier googleTokens) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.googleTokens = googleTokens;
    }
    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = normalize(r.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw conflict();
        }
        User user = new User(
                r.firstName().trim(), r.lastName().trim(), email, encoder.encode(r.password()));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw conflict();
        }
        return tokens(user);
    }
    @Transactional
    public AuthResponse login(LoginRequest r) {
        User user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalize(r.email()))
                            .orElseThrow(this::invalidCredentials);
        if (user.getAuthProvider() != AuthProvider.LOCAL
                || !encoder.matches(r.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return tokens(user);
    }
    @Transactional
    public AuthResponse google(GoogleLoginRequest request) {
        GoogleIdentity identity = googleTokens.verify(request.idToken());
        User subjectUser =
                users.findByGoogleSubjectAndDeletedAtIsNull(identity.subject()).orElse(null);
        if (subjectUser != null) {
            if (!subjectUser.getEmail().equalsIgnoreCase(identity.email())) {
                throw googleFailure();
            }
            return tokens(subjectUser);
        }
        String email = normalize(identity.email());
        User emailUser = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElse(null);
        if (emailUser != null) {
            if (emailUser.getAuthProvider() == AuthProvider.LOCAL) {
                throw linkingRequired();
            }
            throw googleFailure();
        }
        User user = User.google(
                name(identity.givenName()), name(identity.familyName()), email, identity.subject());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw googleFailure();
        }
        return tokens(user);
    }
    @Transactional
    public AuthResponse refresh(RefreshRequest r) {
        var rotation = refreshTokens.rotate(r.refreshToken());
        return response(rotation.user(), rotation.refreshToken());
    }
    @Transactional
    public void logout(RefreshRequest r) {
        refreshTokens.revoke(r.refreshToken());
    }

    private AuthResponse tokens(User user) {
        return response(user, refreshTokens.issue(user).raw());
    }

    private AuthResponse response(User u, String refresh) {
        return new AuthResponse(jwt.generateAccessToken(u), refresh, "Bearer",
                jwt.accessExpirationSeconds(), UserResponse.from(u));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                "Email or password is incorrect");
    }

    private ApiException conflict() {
        return new ApiException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT,
                "An account with this email already exists");
    }

    private String name(String value) {
        return value == null || value.isBlank() ? "Google User" : value.trim();
    }

    private ApiException googleFailure() {
        return new ApiException(ErrorCode.AUTH_GOOGLE_LOGIN_FAILED, HttpStatus.UNAUTHORIZED,
                "Google authentication failed");
    }

    private ApiException linkingRequired() {
        return new ApiException(ErrorCode.AUTH_ACCOUNT_LINKING_REQUIRED, HttpStatus.CONFLICT,
                "A local account already uses this email; sign in with password before linking "
                        + "Google");
    }
}
