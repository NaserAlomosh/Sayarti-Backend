package com.sayarti.backend.user.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record SelectCountryRequest(@NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode) { }
