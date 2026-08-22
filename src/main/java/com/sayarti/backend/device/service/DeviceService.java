package com.sayarti.backend.device.service;

import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.device.dto.DeviceResponse;
import com.sayarti.backend.device.dto.RegisterDeviceRequest;
import com.sayarti.backend.device.dto.UpdateFcmTokenRequest;
import com.sayarti.backend.device.entity.Device;
import com.sayarti.backend.device.repository.DeviceRepository;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {
    private final DeviceRepository devices;
    private final UserRepository users;

    public DeviceService(DeviceRepository devices, UserRepository users) {
        this.devices = devices;
        this.users = users;
    }

    @Transactional
    public DeviceResponse register(AuthenticatedUser authenticated, RegisterDeviceRequest request) {
        requireUser(authenticated);
        String identifier = request.deviceIdentifier().trim();
        String token = request.fcmToken().trim();
        Device device = devices.findByUserIdAndDeviceIdentifier(authenticated.id(), identifier)
                .orElseGet(() -> new Device(authenticated.id(), identifier,
                        request.platform(), token));
        releaseTokenFromOtherDevice(token, device.getId());
        device.refresh(request.platform(), token);
        return DeviceResponse.from(devices.save(device));
    }

    @Transactional
    public DeviceResponse updateToken(AuthenticatedUser authenticated, UUID deviceId,
            UpdateFcmTokenRequest request) {
        Device device = ownedDevice(authenticated, deviceId);
        String token = request.fcmToken().trim();
        releaseTokenFromOtherDevice(token, device.getId());
        device.rotateToken(token);
        return DeviceResponse.from(device);
    }

    @Transactional
    public void delete(AuthenticatedUser authenticated, UUID deviceId) {
        devices.delete(ownedDevice(authenticated, deviceId));
    }

    private void releaseTokenFromOtherDevice(String token, UUID retainedDeviceId) {
        devices.findByFcmToken(token)
                .filter(existing -> !existing.getId().equals(retainedDeviceId))
                .ifPresent(existing -> {
                    devices.delete(existing);
                    devices.flush();
                });
    }

    private Device ownedDevice(AuthenticatedUser authenticated, UUID deviceId) {
        requireUser(authenticated);
        return devices.findByIdAndUserId(deviceId, authenticated.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DEVICE_NOT_FOUND, "Device not found"));
    }

    private void requireUser(AuthenticatedUser authenticated) {
        users.findByIdAndDeletedAtIsNull(authenticated.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND, "User not found"));
    }
}
