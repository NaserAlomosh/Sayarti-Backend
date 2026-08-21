package com.sayarti.backend.user.dto;
import com.sayarti.backend.user.entity.User;
import java.time.Instant;
import java.util.UUID;
public record UserResponse(UUID id, String firstName, String lastName, String email, String authProvider, Instant createdAt) {
    public static UserResponse from(User user) { return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getAuthProvider().name(), user.getCreatedAt()); }
}
