package com.sayarti.backend.notification;

public interface NotificationProvider {
    void send(String registrationToken, NotificationCommand notification)
            throws NotificationProviderException;
}
