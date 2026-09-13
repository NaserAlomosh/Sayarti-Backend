package com.sayarti.backend.statistics.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "General statistics for one active, owned vehicle. With no activity, all "
        + "counts and fuel quantity are zero and all currency-total arrays are empty.")
public record GeneralStatisticsResponse(
        @Schema(description = "Vehicle identifier") UUID vehicleId,
        @Schema(description = "Current vehicle odometer mileage", example = "42000") long currentMileage,
        @Schema(example = "3") long totalFuelRecords,
        @Schema(description = "Sum of non-deleted fuel quantities in liters", example = "95.250")
        BigDecimal totalFuelQuantity,
        @ArraySchema(arraySchema = @Schema(description = "Fuel cost totals separated by currency"))
        List<CurrencyTotalResponse> totalFuelCostByCurrency,
        @Schema(example = "2") long totalMaintenanceRecords,
        @ArraySchema(arraySchema = @Schema(description = "Maintenance cost totals separated by currency"))
        List<CurrencyTotalResponse> totalMaintenanceCostByCurrency,
        @Schema(example = "4") long totalExpenseRecords,
        @ArraySchema(arraySchema = @Schema(description = "Expense amount totals separated by currency"))
        List<CurrencyTotalResponse> totalExpenseAmountByCurrency,
        @Schema(description = "Non-deleted, incomplete reminders", example = "2") long activeReminderCount,
        @Schema(description = "Non-deleted, completed reminders", example = "1") long completedReminderCount) {
}
