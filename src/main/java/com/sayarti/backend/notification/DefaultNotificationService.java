package com.sayarti.backend.notification;

import com.sayarti.backend.device.entity.Device;
import com.sayarti.backend.device.repository.DeviceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class DefaultNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final DeviceRepository devices;
    private final NotificationProvider provider;

    DefaultNotificationService(DeviceRepository devices, NotificationProvider provider) {
        this.devices = devices;
        this.provider = provider;
    }

    @Override
    public NotificationDelivery sendToDevice(Device device, NotificationCommand notification) {
        String token = device.getFcmToken();
        if (token == null || token.isBlank()) {
            return new NotificationDelivery(device.getId(),
                    NotificationDelivery.Status.SKIPPED_NO_TOKEN);
        }

        try {
            provider.send(token, notification);
            return new NotificationDelivery(device.getId(), NotificationDelivery.Status.SENT);
        } catch (NotificationProviderException exception) {
            if (exception.isPermanentlyInvalidToken()) {
                devices.delete(device);
                log.info("Removed device registration after permanent provider rejection; deviceId={}",
                        device.getId());
                return new NotificationDelivery(device.getId(),
                        NotificationDelivery.Status.PERMANENTLY_INVALID_TOKEN);
            }
            log.warn("Notification delivery failed; deviceId={}, failureType={}",
                    device.getId(), exception.getClass().getSimpleName());
            return new NotificationDelivery(device.getId(), NotificationDelivery.Status.FAILED);
        }
    }

    @Override
    public UserNotificationResult sendToUser(UUID userId, NotificationCommand notification) {
        List<NotificationDelivery> deliveries = new ArrayList<>();
        for (Device device : devices.findAllByUserId(userId)) {
            deliveries.add(sendToDevice(device, notification));
        }
        return new UserNotificationResult(deliveries);
    }
}
