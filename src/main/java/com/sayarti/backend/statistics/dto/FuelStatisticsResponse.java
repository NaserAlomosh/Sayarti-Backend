package com.sayarti.backend.statistics.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fuel statistics for one active, owned vehicle. An empty fuel history returns "
        + "zero records, quantities, and distance, empty currency arrays, and null efficiency values. "
        + "Efficiency values are also null when the history has insufficient valid odometer intervals.")
public record FuelStatisticsResponse(
        @Schema(description = "Vehicle identifier") UUID vehicleId,
        @Schema(description = "Number of active fuel records", example = "3") long totalFuelRecords,
        @Schema(description = "Total fuel quantity in liters", example = "100.000")
        BigDecimal totalFuelQuantity,
        @ArraySchema(arraySchema = @Schema(description = "Fuel costs separated by original currency"))
        List<CurrencyTotalResponse> totalFuelCostByCurrency,
        @Schema(description = "Distance across valid consecutive refill intervals in kilometers",
                example = "800.00") BigDecimal totalDistanceKm,
        @Schema(description = "Distance divided by fuel used on valid intervals; null when unavailable",
                example = "11.4286", nullable = true)
        BigDecimal averageFuelEfficiencyKmPerLiter,
        @Schema(description = "Fuel used per 100 km on valid intervals; null when unavailable",
                example = "8.7500", nullable = true)
        BigDecimal averageFuelConsumptionLitersPer100Km,
        @ArraySchema(arraySchema = @Schema(description = "Cost per kilometer separated by original "
                + "currency; amounts are null when distance is unavailable"))
        List<CurrencyRateResponse> averageFuelCostPerKmByCurrency) {
}
