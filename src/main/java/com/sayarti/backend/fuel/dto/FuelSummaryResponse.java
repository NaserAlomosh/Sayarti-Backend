package com.sayarti.backend.fuel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Schema(description = "Derived, non-persisted fuel calculations. Records are ordered by filledAt, "
        + "then odometer, creation time, and id. Each strictly increasing adjacent odometer pair "
        + "contributes its distance and the later record's quantity. Consumption values are null "
        + "when no valid pair exists or for electric-only vehicles.")
public record FuelSummaryResponse(
        UUID vehicleId,
        boolean liquidFuelCalculationsSupported,
        YearMonth month,
        @Schema(description = "Sum of quantities from all active records, in liters")
        BigDecimal totalFuelQuantity,
        @Schema(description = "Sum of positive distances between eligible consecutive records, in km")
        BigDecimal totalDistanceKm,
        @Schema(description = "Aggregate totalDistanceKm / eligible fuel quantity; null when unavailable")
        BigDecimal averageFuelEfficiencyKmPerLiter,
        @Schema(description = "Eligible fuel quantity / totalDistanceKm * 100; null when unavailable")
        BigDecimal averageLitersPer100Km,
        @Schema(description = "Costs remain separated by source currency; no conversion is performed")
        List<FuelCostSummary> costsByCurrency) {
}
