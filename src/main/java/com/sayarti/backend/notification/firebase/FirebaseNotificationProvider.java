package com.sayarti.backend.notification.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.sayarti.backend.notification.NotificationCommand;
import com.sayarti.backend.notification.NotificationProvider;
import com.sayarti.backend.notification.NotificationProviderException;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Component
@ConditionalOnBean(FirebaseMessaging.class)
class FirebaseNotificationProvider implements NotificationProvider {
    private final FirebaseMessaging messaging;

    FirebaseNotificationProvider(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public void send(String registrationToken, NotificationCommand notification)
            throws NotificationProviderException {
        Message message = Message.builder()
                .setToken(registrationToken)
                .setNotification(Notification.builder()
                        .setTitle(notification.title())
                        .setBody(notification.body())
                        .build())
                .putAllData(notification.data())
                .build();
        try {
            messaging.send(message);
        } catch (FirebaseMessagingException exception) {
            if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                throw NotificationProviderException.permanentlyInvalidToken(exception);
            }
            throw NotificationProviderException.temporaryOrUnknown(exception);
        }
    }
}
