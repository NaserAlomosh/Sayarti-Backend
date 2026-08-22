package com.sayarti.backend.notification;

import com.sayarti.backend.device.entity.Device;
import java.util.UUID;

public interface NotificationService {
    NotificationDelivery sendToDevice(Device device, NotificationCommand notification);

    UserNotificationResult sendToUser(UUID userId, NotificationCommand notification);
}
