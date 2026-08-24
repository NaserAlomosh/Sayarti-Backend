package com.sayarti.backend.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "One provider-neutral vehicle timeline entry. Monetary fields are null "
        + "for reminders and retain the source record's original currency otherwise.")
public record VehicleActivityResponse(
        ActivityType type,
        UUID referenceId,
        UUID vehicleId,
        String title,
        Instant occurredAt,
        @Schema(nullable = true) BigDecimal amount,
        @Schema(nullable = true) String currencyCode) { }
