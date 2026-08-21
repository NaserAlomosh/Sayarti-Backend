package com.sayarti.backend.auth.dto;
import jakarta.validation.constraints.*;
public record LoginRequest(@NotBlank @Email @Size(max=320) String email, @NotBlank @Size(max=72) String password) {}
