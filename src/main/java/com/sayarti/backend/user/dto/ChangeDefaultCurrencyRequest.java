package com.sayarti.backend.user.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record ChangeDefaultCurrencyRequest(@NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currencyCode) { }
