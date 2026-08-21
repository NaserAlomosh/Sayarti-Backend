package com.sayarti.backend.auth.dto;
import jakarta.validation.constraints.*;
public record RegisterRequest(
 @NotBlank @Size(max=100) String firstName,
 @NotBlank @Size(max=100) String lastName,
 @NotBlank @Email @Size(max=320) String email,
 @NotBlank @Size(min=8,max=72) @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message="must contain upper-case, lower-case, and numeric characters") String password) {}
