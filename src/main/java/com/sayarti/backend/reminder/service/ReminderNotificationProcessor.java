package com.sayarti.backend.reminder.service;

import com.sayarti.backend.notification.NotificationCommand;
import com.sayarti.backend.notification.NotificationService;
import com.sayarti.backend.notification.UserNotificationResult;
import com.sayarti.backend.reminder.entity.Reminder;
import com.sayarti.backend.reminder.entity.ReminderTriggerType;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.vehicle.entity.Vehicle;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivers a one-shot reminder notification while holding a database row lock. Notification
 * delivery and user completion are deliberately independent: delivery records that at least one
 * registered device accepted the message, while completion remains an explicit user action. A
 * partial multi-device result is considered delivered so devices that already succeeded are not
 * notified again; a result with no successful device remains eligible for a later poll.
 */
@Service
public class ReminderNotificationProcessor {
    private final ReminderRepository reminders;
    private final VehicleRepository vehicles;
    private final NotificationService notifications;
    private final Clock clock;

    @Autowired
    public ReminderNotificationProcessor(ReminderRepository reminders, VehicleRepository vehicles,
            NotificationService notifications) {
        this(reminders, vehicles, notifications, Clock.systemUTC());
    }

    ReminderNotificationProcessor(ReminderRepository reminders, VehicleRepository vehicles,
            NotificationService notifications, Clock clock) {
        this.reminders = reminders;
        this.vehicles = vehicles;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public boolean process(UUID reminderId) {
        Reminder reminder = reminders.findByIdForNotification(reminderId).orElse(null);
        if (reminder == null || reminder.isCompleted() || reminder.getDeletedAt() != null
                || reminder.getNotificationDeliveredAt() != null) {
            return false;
        }
        Vehicle vehicle = vehicles.findById(reminder.getVehicleId()).orElse(null);
        Instant now = clock.instant();
        if (vehicle == null || vehicle.getDeletedAt() != null || !isDue(reminder, vehicle, now)) {
            return false;
        }

        NotificationCommand command = new NotificationCommand(reminder.getTitle(),
                notificationBody(reminder, vehicle), Map.of(
                        "type", "REMINDER",
                        "reminderId", reminder.getId().toString(),
                        "vehicleId", vehicle.getId().toString()));
        UserNotificationResult result = notifications.sendToUser(vehicle.getUserId(), command);
        if (result.sentCount() == 0) {
            return false;
        }
        reminder.markNotificationDelivered(now);
        return true;
    }

    static boolean isDue(Reminder reminder, Vehicle vehicle, Instant now) {
        if (reminder.getTriggerType() == ReminderTriggerType.DATE) {
            return reminder.getTargetDate() != null && !reminder.getTargetDate().isAfter(now);
        }
        return reminder.getTargetMileage() != null
                && vehicle.getCurrentMileage() >= reminder.getTargetMileage();
    }

    private String notificationBody(Reminder reminder, Vehicle vehicle) {
        if (reminder.getDescription() != null && !reminder.getDescription().isBlank()) {
            return reminder.getDescription();
        }
        return reminder.getTriggerType() == ReminderTriggerType.MILEAGE
                ? "Vehicle mileage has reached " + reminder.getTargetMileage() + " km."
                : "This vehicle reminder is now due.";
    }
}
