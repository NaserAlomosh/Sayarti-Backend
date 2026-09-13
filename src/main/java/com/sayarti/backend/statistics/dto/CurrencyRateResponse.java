package com.sayarti.backend.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "A per-distance monetary rate in one original currency. Currencies are never converted.")
public record CurrencyRateResponse(
        @Schema(description = "Existing ISO 4217 currency code", example = "USD") String currencyCode,
        @Schema(description = "Average cost per kilometer, or null when distance cannot be calculated",
                example = "0.2750", nullable = true)
        BigDecimal amountPerKm) {
}
