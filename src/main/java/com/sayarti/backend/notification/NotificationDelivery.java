package com.sayarti.backend.notification;

import java.util.UUID;

public record NotificationDelivery(UUID deviceId, Status status) {
    public enum Status {
        SENT,
        SKIPPED_NO_TOKEN,
        PERMANENTLY_INVALID_TOKEN,
        FAILED
    }
}
