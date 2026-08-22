package com.sayarti.backend.fuel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record FuelCostSummary(
        @Schema(description = "ISO 4217 currency retained by the source fuel records")
        String currencyCode,
        @Schema(description = "Total active fuel-record cost in this currency")
        BigDecimal totalFuelCost,
        @Schema(description = "Cost in this currency for the requested UTC calendar month")
        BigDecimal monthlyFuelCost,
        @Schema(description = "Total cost divided by aggregate eligible distance; null when distance is unavailable")
        BigDecimal costPerKm) {
}
