package com.sayarti.backend.device.dto;

import com.sayarti.backend.device.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank @Size(max = 255) String deviceIdentifier,
        @NotNull DevicePlatform platform,
        @NotBlank @Size(max = 512) String fcmToken) { }
