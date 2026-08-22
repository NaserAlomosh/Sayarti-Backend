package com.sayarti.backend.reminder.dto;

import com.sayarti.backend.reminder.entity.Reminder;
import com.sayarti.backend.reminder.entity.ReminderCategory;
import com.sayarti.backend.reminder.entity.ReminderTriggerType;
import java.time.Instant;
import java.util.UUID;

public record ReminderResponse(UUID id, UUID vehicleId, ReminderCategory category, String title,
        String description, ReminderTriggerType triggerType, Instant targetDate,
        Long targetMileage, boolean completed, Instant completedAt, Instant createdAt,
        Instant updatedAt) {
    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(reminder.getId(), reminder.getVehicleId(),
                reminder.getCategory(), reminder.getTitle(), reminder.getDescription(),
                reminder.getTriggerType(), reminder.getTargetDate(), reminder.getTargetMileage(),
                reminder.isCompleted(), reminder.getCompletedAt(), reminder.getCreatedAt(),
                reminder.getUpdatedAt());
    }
}
