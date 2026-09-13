package com.sayarti.backend.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "An average monetary amount in one original currency. Currencies are never converted.")
public record CurrencyAverageResponse(
        @Schema(description = "Existing ISO 4217 currency code", example = "USD") String currencyCode,
        @Schema(description = "Arithmetic mean in that currency", example = "62.7500")
        BigDecimal amount) {
}
