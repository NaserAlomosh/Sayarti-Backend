package com.sayarti.backend.reminder.dto;

import com.sayarti.backend.reminder.entity.ReminderCategory;
import com.sayarti.backend.reminder.entity.ReminderTriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateReminderRequest(
        @NotNull ReminderCategory category,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull ReminderTriggerType triggerType,
        Instant targetDate,
        @PositiveOrZero Long targetMileage) { }
