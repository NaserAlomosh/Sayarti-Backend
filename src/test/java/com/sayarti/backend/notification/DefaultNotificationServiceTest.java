package com.sayarti.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sayarti.backend.device.entity.Device;
import com.sayarti.backend.device.entity.DevicePlatform;
import com.sayarti.backend.device.repository.DeviceRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNotificationServiceTest {
    @Mock
    private DeviceRepository devices;
    @Mock
    private NotificationProvider provider;

    private DefaultNotificationService service;
    private NotificationCommand notification;

    @BeforeEach
    void setUp() {
        service = new DefaultNotificationService(devices, provider);
        notification = new NotificationCommand("Service due", "Book maintenance",
                Map.of("vehicleId", "123"));
    }

    @Test
    void sendsNotificationToOneValidDevice() throws Exception {
        Device device = device("token-one");

        NotificationDelivery result = service.sendToDevice(device, notification);

        assertThat(result.status()).isEqualTo(NotificationDelivery.Status.SENT);
        verify(provider).send("token-one", notification);
    }

    @Test
    void sendsToEveryUsableUserDeviceAndSkipsMissingTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        Device first = device("token-one");
        Device second = device("token-two");
        Device missing = org.mockito.Mockito.mock(Device.class);
        when(missing.getId()).thenReturn(UUID.randomUUID());
        when(missing.getFcmToken()).thenReturn(" ");
        when(devices.findAllByUserId(userId)).thenReturn(List.of(first, missing, second));

        UserNotificationResult result = service.sendToUser(userId, notification);

        assertThat(result.sentCount()).isEqualTo(2);
        assertThat(result.deliveries()).extracting(NotificationDelivery::status)
                .containsExactly(NotificationDelivery.Status.SENT,
                        NotificationDelivery.Status.SKIPPED_NO_TOKEN,
                        NotificationDelivery.Status.SENT);
        verify(provider).send("token-one", notification);
        verify(provider).send("token-two", notification);
    }

    @Test
    void partialFailurePreservesSuccessfulDeliveries() throws Exception {
        UUID userId = UUID.randomUUID();
        Device first = device("token-one");
        Device second = device("token-two");
        when(devices.findAllByUserId(userId)).thenReturn(List.of(first, second));
        org.mockito.Mockito.doThrow(NotificationProviderException.temporaryOrUnknown(
                new RuntimeException("network"))).when(provider).send("token-one", notification);

        UserNotificationResult result = service.sendToUser(userId, notification);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.sentCount()).isEqualTo(1);
        verify(provider).send("token-two", notification);
    }

    @Test
    void removesDeviceForPermanentlyInvalidToken() throws Exception {
        Device device = device("expired-token");
        org.mockito.Mockito.doThrow(NotificationProviderException.permanentlyInvalidToken(
                new RuntimeException("unregistered")))
                .when(provider).send("expired-token", notification);

        NotificationDelivery result = service.sendToDevice(device, notification);

        assertThat(result.status())
                .isEqualTo(NotificationDelivery.Status.PERMANENTLY_INVALID_TOKEN);
        verify(devices).delete(device);
    }

    @Test
    void temporaryFailureDoesNotDeleteDevice() throws Exception {
        Device device = device("valid-token");
        org.mockito.Mockito.doThrow(NotificationProviderException.temporaryOrUnknown(
                new RuntimeException("timeout")))
                .when(provider).send("valid-token", notification);

        NotificationDelivery result = service.sendToDevice(device, notification);

        assertThat(result.status()).isEqualTo(NotificationDelivery.Status.FAILED);
        verify(devices, never()).delete(device);
    }

    private Device device(String token) {
        return new Device(UUID.randomUUID(), UUID.randomUUID().toString(),
                DevicePlatform.ANDROID, token);
    }
}
