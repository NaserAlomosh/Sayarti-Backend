package com.sayarti.backend.notification;

import java.util.List;

public record UserNotificationResult(List<NotificationDelivery> deliveries) {
    public UserNotificationResult {
        deliveries = List.copyOf(deliveries);
    }

    public long sentCount() {
        return count(NotificationDelivery.Status.SENT);
    }

    public long failedCount() {
        return count(NotificationDelivery.Status.FAILED);
    }

    private long count(NotificationDelivery.Status status) {
        return deliveries.stream().filter(delivery -> delivery.status() == status).count();
    }
}
