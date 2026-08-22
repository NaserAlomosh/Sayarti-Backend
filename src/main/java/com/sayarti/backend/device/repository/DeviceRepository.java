package com.sayarti.backend.device.repository;

import com.sayarti.backend.device.entity.Device;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByUserIdAndDeviceIdentifier(UUID userId, String deviceIdentifier);
    Optional<Device> findByIdAndUserId(UUID id, UUID userId);
    Optional<Device> findByFcmToken(String fcmToken);
}
