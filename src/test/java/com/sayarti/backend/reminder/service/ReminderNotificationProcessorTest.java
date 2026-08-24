package com.sayarti.backend.reminder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sayarti.backend.i18n.MessageLocalizer;
import com.sayarti.backend.notification.NotificationCommand;
import com.sayarti.backend.notification.NotificationDelivery;
import com.sayarti.backend.notification.NotificationService;
import com.sayarti.backend.notification.UserNotificationResult;
import com.sayarti.backend.reminder.entity.Reminder;
import com.sayarti.backend.reminder.entity.ReminderCategory;
import com.sayarti.backend.reminder.entity.ReminderTriggerType;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.vehicle.entity.PowertrainType;
import com.sayarti.backend.vehicle.entity.Vehicle;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

@ExtendWith(MockitoExtension.class)
class ReminderNotificationProcessorTest {
    private static final Instant NOW = Instant.parse("2026-08-22T12:00:00Z");
    @Mock ReminderRepository reminders;
    @Mock VehicleRepository vehicles;
    @Mock NotificationService notifications;
    @Mock UserRepository users;
    ReminderNotificationProcessor processor;
    Vehicle vehicle;

    @BeforeEach
    void setUp() {
        processor = new ReminderNotificationProcessor(reminders, vehicles, notifications,
                Clock.fixed(NOW, ZoneOffset.UTC));
        vehicle = new Vehicle(UUID.randomUUID(), "Toyota", "Corolla", 2024,
                PowertrainType.GASOLINE, 10_000, null, null, null, null, null, null, null);
    }

    @Test void dateReminderBecomesDue() {
        assertDelivered(date(NOW.minusSeconds(1)), sent());
    }

    @Test void dueLicenseExpirationDateReminderIsSelected() {
        assertDelivered(expiration(ReminderCategory.LICENSE_EXPIRATION, NOW), sent());
    }

    @Test void dueInsuranceExpirationDateReminderIsSelected() {
        assertDelivered(expiration(ReminderCategory.INSURANCE_EXPIRATION, NOW), sent());
    }

    @Test void futureLicenseAndInsuranceExpirationRemindersAreNotSelectedEarly() {
        assertNotDue(expiration(ReminderCategory.LICENSE_EXPIRATION, NOW.plusSeconds(1)));
        org.mockito.Mockito.clearInvocations(notifications);
        assertNotDue(expiration(ReminderCategory.INSURANCE_EXPIRATION, NOW.plusSeconds(1)));
    }

    @Test void dateReminderNotYetDue() {
        assertNotDue(date(NOW.plusSeconds(1)));
    }

    @Test void mileageReminderBecomesDue() {
        assertDelivered(mileage(10_000), sent());
    }

    @Test void mileageReminderNotYetDue() {
        assertNotDue(mileage(10_001));
    }

    @Test void completedReminderIsIgnored() {
        Reminder reminder = date(NOW.minusSeconds(1));
        reminder.complete();
        assertIgnored(reminder);
    }

    @Test void deletedReminderIsIgnored() {
        Reminder reminder = mileage(1);
        reminder.delete();
        assertIgnored(reminder);
    }

    @Test void alreadyNotifiedReminderIsIgnored() {
        Reminder reminder = date(NOW);
        reminder.markNotificationDelivered(NOW.minusSeconds(10));
        assertIgnored(reminder);
    }

    @Test void repeatedRunsDoNotSendAgain() {
        Reminder reminder = date(NOW);
        arrange(reminder);
        when(notifications.sendToUser(any(), any())).thenReturn(sent());
        assertThat(processor.process(reminder.getId())).isTrue();
        assertThat(processor.process(reminder.getId())).isFalse();
        verify(notifications).sendToUser(any(), any());
    }

    @Test void temporaryFailureRemainsRetryable() {
        Reminder reminder = mileage(1);
        arrange(reminder);
        when(notifications.sendToUser(any(), any())).thenReturn(result(
                NotificationDelivery.Status.FAILED));
        assertThat(processor.process(reminder.getId())).isFalse();
        assertThat(reminder.getNotificationDeliveredAt()).isNull();
        assertThat(processor.process(reminder.getId())).isFalse();
        verify(notifications, times(2)).sendToUser(any(), any());
    }

    @Test void successfulDeliveryPersistsState() {
        Reminder reminder = date(NOW);
        assertDelivered(reminder, sent());
        assertThat(reminder.getNotificationDeliveredAt()).isEqualTo(NOW);
    }

    @Test void partialMultiDeviceSuccessCountsAsDelivered() {
        Reminder reminder = mileage(1);
        assertDelivered(reminder, new UserNotificationResult(List.of(
                new NotificationDelivery(UUID.randomUUID(), NotificationDelivery.Status.SENT),
                new NotificationDelivery(UUID.randomUUID(), NotificationDelivery.Status.FAILED))));
    }

    @Test void licenseExpirationUsesEnglishLocalizationAndKeepsAuthoredTitle() {
        assertLocalized(ReminderCategory.LICENSE_EXPIRATION, "en", "Renew my license",
                "Your vehicle license expiration reminder is now due.");
    }

    @Test void insuranceExpirationUsesArabicPreferredLanguage() {
        assertLocalized(ReminderCategory.INSURANCE_EXPIRATION, "ar", "تأميني",
                "حان الآن موعد تذكير انتهاء تأمين مركبتك.");
    }

    private void assertLocalized(ReminderCategory category, String language, String title,
            String expectedBody) {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        processor = new ReminderNotificationProcessor(reminders, vehicles, notifications,
                Clock.fixed(NOW, ZoneOffset.UTC), users, new MessageLocalizer(source));
        User user = new User("Reminder", "Owner", "owner@example.com", "hash");
        user.changePreferredLanguage(language);
        when(users.findById(vehicle.getUserId())).thenReturn(Optional.of(user));
        Reminder reminder = new Reminder(vehicle.getId(), category, title, "authored description",
                ReminderTriggerType.DATE, NOW, null);
        arrange(reminder);
        when(notifications.sendToUser(any(), any())).thenReturn(sent());

        assertThat(processor.process(reminder.getId())).isTrue();

        ArgumentCaptor<NotificationCommand> command = forClass(NotificationCommand.class);
        verify(notifications).sendToUser(any(), command.capture());
        assertThat(command.getValue().title()).isEqualTo(title);
        assertThat(command.getValue().body()).isEqualTo(expectedBody);
    }

    private void assertDelivered(Reminder reminder, UserNotificationResult result) {
        arrange(reminder);
        when(notifications.sendToUser(any(), any())).thenReturn(result);
        assertThat(processor.process(reminder.getId())).isTrue();
        assertThat(reminder.getNotificationDeliveredAt()).isEqualTo(NOW);
    }

    private void assertIgnored(Reminder reminder) {
        when(reminders.findByIdForNotification(reminder.getId())).thenReturn(Optional.of(reminder));
        assertThat(processor.process(reminder.getId())).isFalse();
        verify(notifications, never()).sendToUser(any(), any());
    }

    private void assertNotDue(Reminder reminder) {
        arrange(reminder);
        assertThat(processor.process(reminder.getId())).isFalse();
        verify(notifications, never()).sendToUser(any(), any());
    }

    private void arrange(Reminder reminder) {
        when(reminders.findByIdForNotification(reminder.getId())).thenReturn(Optional.of(reminder));
        when(vehicles.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
    }

    private Reminder date(Instant target) {
        return new Reminder(vehicle.getId(), ReminderCategory.CUSTOM, "Reminder", null,
                ReminderTriggerType.DATE, target, null);
    }

    private Reminder mileage(long target) {
        return new Reminder(vehicle.getId(), ReminderCategory.MAINTENANCE, "Service", null,
                ReminderTriggerType.MILEAGE, null, target);
    }

    private Reminder expiration(ReminderCategory category, Instant target) {
        return new Reminder(vehicle.getId(), category, "Expiration", null,
                ReminderTriggerType.DATE, target, null);
    }

    private UserNotificationResult sent() {
        return result(NotificationDelivery.Status.SENT);
    }

    private UserNotificationResult result(NotificationDelivery.Status status) {
        return new UserNotificationResult(List.of(
                new NotificationDelivery(UUID.randomUUID(), status)));
    }
}
