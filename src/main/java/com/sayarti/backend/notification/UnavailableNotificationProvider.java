package com.sayarti.backend.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(NotificationProvider.class)
class UnavailableNotificationProvider implements NotificationProvider {
    @Override
    public void send(String registrationToken, NotificationCommand notification)
            throws NotificationProviderException {
        throw NotificationProviderException.temporaryOrUnknown(
                new IllegalStateException("Firebase notification delivery is not configured"));
    }
}
