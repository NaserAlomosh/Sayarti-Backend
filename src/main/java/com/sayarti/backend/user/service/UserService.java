package com.sayarti.backend.user.service;

import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.user.dto.UserResponse;
import com.sayarti.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) {
        this.users = users;
    }

    public UserResponse current(AuthenticatedUser principal) {
        return users.findById(principal.id())
                .map(UserResponse::from)
                .orElseThrow(()
                                     -> new ResourceNotFoundException(
                                             ErrorCode.USER_NOT_FOUND, "User not found"));
    }
}
