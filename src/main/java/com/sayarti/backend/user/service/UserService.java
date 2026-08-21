package com.sayarti.backend.user.service;

import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.user.dto.UpdateUserRequest;
import com.sayarti.backend.user.dto.UserResponse;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;

    public UserService(UserRepository users, RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.refreshTokens = refreshTokens;
    }

    @Transactional(readOnly = true)
    public UserResponse current(AuthenticatedUser principal) {
        return UserResponse.from(activeUser(principal));
    }

    @Transactional
    public UserResponse update(AuthenticatedUser principal, UpdateUserRequest request) {
        User user = activeUser(principal);
        user.updateProfile(request.firstName(), request.lastName());
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(AuthenticatedUser principal) {
        User user = activeUser(principal);
        user.delete();
        refreshTokens.revokeAllActiveByUserId(user.getId(), Instant.now());
    }

    private User activeUser(AuthenticatedUser principal) {
        return users.findByIdAndDeletedAtIsNull(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND, "User not found"));
    }
}
