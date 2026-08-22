package com.sayarti.backend.notification.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.sayarti.backend.notification.NotificationCommand;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FirebaseNotificationProviderTest {
    @Mock
    private FirebaseMessaging messaging;

    @Test
    void buildsFirebaseMessageAndDelegatesWithoutNetworkAccess() throws Exception {
        FirebaseNotificationProvider provider = new FirebaseNotificationProvider(messaging);

        provider.send("registration-token", new NotificationCommand(
                "Insurance due", "Renew soon", Map.of("reminderId", "42")));

        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(messaging).send(message.capture());
        assertThat(message.getValue()).isNotNull();
    }
}
