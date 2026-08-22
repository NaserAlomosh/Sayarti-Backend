package com.sayarti.backend.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFcmTokenRequest(
        @NotBlank @Size(max = 512) String fcmToken) { }
