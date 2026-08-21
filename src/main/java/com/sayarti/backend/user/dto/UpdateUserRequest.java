package com.sayarti.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties({
    "id",
    "email",
    "passwordHash",
    "authProvider",
    "googleSubject",
    "createdAt",
    "updatedAt",
    "deletedAt"
})
public record UpdateUserRequest(
        @Size(max = 100)
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String firstName,
        @Size(max = 100)
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String lastName) {

    @AssertTrue(message = "at least one editable field must be provided")
    public boolean isAnyFieldProvided() {
        return firstName != null || lastName != null;
    }
}
