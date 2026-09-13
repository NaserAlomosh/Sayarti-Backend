package com.sayarti.backend.notification;

import java.util.Map;

public record NotificationCommand(String title, String body, Map<String, String> data) {
    public NotificationCommand {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Notification title must not be blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Notification body must not be blank");
        }
        data = data == null ? Map.of() : Map.copyOf(data);
        if (data.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                || entry.getKey().isBlank() || entry.getValue() == null)) {
            throw new IllegalArgumentException("Notification data must contain non-blank keys and string values");
        }
    }
}
