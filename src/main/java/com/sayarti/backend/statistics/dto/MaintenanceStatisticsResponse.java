package com.sayarti.backend.statistics.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Maintenance statistics for one active, owned vehicle. An empty maintenance "
        + "history returns zero records, empty cost and category arrays, and null latest values.")
public record MaintenanceStatisticsResponse(
        @Schema(description = "Vehicle identifier") UUID vehicleId,
        @Schema(description = "Number of active maintenance records", example = "3")
        long totalMaintenanceRecords,
        @ArraySchema(arraySchema = @Schema(description = "Maintenance costs separated by original currency"))
        List<CurrencyTotalResponse> totalMaintenanceCostByCurrency,
        @ArraySchema(arraySchema = @Schema(description = "Average maintenance cost separated by original currency"))
        List<CurrencyAverageResponse> averageMaintenanceCostByCurrency,
        @Schema(description = "Service date of the latest active record", nullable = true,
                example = "2026-08-20T10:00:00Z")
        Instant latestMaintenanceDate,
        @Schema(description = "Mileage on the latest active record", nullable = true,
                example = "42000.00")
        BigDecimal latestMaintenanceMileage,
        @ArraySchema(arraySchema = @Schema(description = "Categories in enum declaration order; "
                + "categories without records are omitted"))
        List<MaintenanceCategoryStatisticsResponse> maintenanceByCategory) {
}
