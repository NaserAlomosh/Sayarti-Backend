package com.sayarti.backend.fuel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateFuelRecordRequest(
        @NotNull @DecimalMin("0.0") @Digits(integer = 17, fraction = 2) BigDecimal odometerKm,
        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 9, fraction = 3) BigDecimal quantityLiters,
        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 15, fraction = 4) BigDecimal pricePerLiter,
        @Size(min = 3, max = 3) String currencyCode,
        @NotNull Instant filledAt,
        @NotNull Boolean fullTank,
        @Size(max = 200) String stationName,
        @Size(max = 2000) String notes) { }
