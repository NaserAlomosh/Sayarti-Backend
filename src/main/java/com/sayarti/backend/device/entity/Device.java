package com.sayarti.backend.device.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class Device {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "device_identifier", nullable = false, length = 255)
    private String deviceIdentifier;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;
    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Device() {
    }

    public Device(UUID userId, String deviceIdentifier, DevicePlatform platform, String fcmToken) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.deviceIdentifier = deviceIdentifier.trim();
        this.platform = platform;
        this.fcmToken = fcmToken.trim();
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void refresh(DevicePlatform platform, String fcmToken) {
        this.platform = platform;
        this.fcmToken = fcmToken.trim();
        this.updatedAt = Instant.now();
    }

    public void rotateToken(String fcmToken) {
        this.fcmToken = fcmToken.trim();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDeviceIdentifier() { return deviceIdentifier; }
    public DevicePlatform getPlatform() { return platform; }
    public String getFcmToken() { return fcmToken; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
