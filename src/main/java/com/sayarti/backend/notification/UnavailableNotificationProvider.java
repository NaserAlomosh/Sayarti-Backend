package com.sayarti.backend.notification;

public class UnavailableNotificationProvider implements NotificationProvider {
    @Override
    public void send(String registrationToken, NotificationCommand notification)
            throws NotificationProviderException {
        throw NotificationProviderException.temporaryOrUnknown(
                new IllegalStateException("Firebase notification delivery is not configured"));
    }
}
