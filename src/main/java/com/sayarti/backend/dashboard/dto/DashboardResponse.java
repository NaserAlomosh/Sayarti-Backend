package com.sayarti.backend.dashboard.dto;

import com.sayarti.backend.reminder.dto.ReminderResponse;
import com.sayarti.backend.statistics.dto.CurrencyAverageResponse;
import com.sayarti.backend.statistics.dto.CurrencyRateResponse;
import com.sayarti.backend.statistics.dto.CurrencyTotalResponse;
import com.sayarti.backend.vehicle.dto.VehicleResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Flutter-ready overview of one active, owned vehicle. Empty domains use "
        + "zero counts, scaled zero quantities, empty currency arrays, and null latest/upcoming "
        + "or insufficient-data calculations. Monetary values retain their original currency.")
public record DashboardResponse(
        UUID vehicleId,
        VehicleResponse vehicle,
        @Schema(description = "Current odometer mileage") long currentMileage,
        FuelSummary fuel,
        MaintenanceSummary maintenance,
        ExpenseSummary expenses,
        TotalCostSummary totalVehicleCost,
        ReminderSummary reminders) {

    public record FuelSummary(
            long totalRecords,
            @Schema(description = "Total liters, scale 3") BigDecimal totalQuantityLiters,
            List<CurrencyTotalResponse> costsByCurrency,
            @Schema(nullable = true) BigDecimal averageKmPerLiter,
            @Schema(nullable = true) BigDecimal averageLitersPer100Km) { }

    public record MaintenanceSummary(
            long totalRecords,
            @Schema(nullable = true) Instant latestMaintenanceDate,
            List<CurrencyTotalResponse> costsByCurrency) { }

    public record ExpenseSummary(long totalRecords,
            List<CurrencyTotalResponse> costsByCurrency) { }

    public record TotalCostSummary(
            List<CurrencyTotalResponse> costsByCurrency,
            List<CurrencyAverageResponse> averageMonthlyCostsByCurrency,
            @Schema(description = "Amounts are null when positive fuel-odometer distance is unavailable")
            List<CurrencyRateResponse> costsPerKilometerByCurrency) { }

    public record ReminderSummary(
            long activeCount,
            long completedCount,
            @Schema(description = "Earliest future date reminder; if none, the nearest reminder "
                    + "above current mileage; null when neither exists", nullable = true)
            ReminderResponse nearestUpcoming) { }
}
