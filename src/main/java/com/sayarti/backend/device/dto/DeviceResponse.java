package com.sayarti.backend.device.dto;

import com.sayarti.backend.device.entity.Device;
import com.sayarti.backend.device.entity.DevicePlatform;
import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(UUID id, String deviceIdentifier, DevicePlatform platform,
        Instant createdAt, Instant updatedAt) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(device.getId(), device.getDeviceIdentifier(),
                device.getPlatform(), device.getCreatedAt(), device.getUpdatedAt());
    }
}
