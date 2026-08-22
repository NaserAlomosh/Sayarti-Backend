package com.sayarti.backend.reminder.dto;

import com.sayarti.backend.reminder.entity.ReminderCategory;
import com.sayarti.backend.reminder.entity.ReminderTriggerType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateReminderRequest(
        ReminderCategory category,
        @Size(max = 200) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String title,
        @Size(max = 2000) String description,
        ReminderTriggerType triggerType,
        Instant targetDate,
        @PositiveOrZero Long targetMileage) { }
