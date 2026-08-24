package com.sayarti.backend.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "True vehicle cost aggregated without currency conversion")
public record TrueVehicleCostResponse(
        UUID vehicleId,
        BigDecimal currentMileage,
        List<CurrencyTotalResponse> totalFuelCostByCurrency,
        List<CurrencyTotalResponse> totalMaintenanceCostByCurrency,
        List<CurrencyTotalResponse> totalExpenseAmountByCurrency,
        List<CurrencyTotalResponse> totalVehicleCostByCurrency,
        @Schema(description = "Total cost divided by the inclusive number of UTC calendar months "
                + "between the earliest and latest active cost record")
        List<CurrencyAverageResponse> averageMonthlyCostByCurrency,
        @Schema(description = "Total cost divided by FuelCalculator eligible distance; rates are "
                + "null when no positive consecutive fuel-record odometer distance exists")
        List<CurrencyRateResponse> costPerKilometerByCurrency) {
}
