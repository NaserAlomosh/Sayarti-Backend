package com.sayarti.backend.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "A monetary total in one original currency. Currencies are never converted.")
public record CurrencyTotalResponse(
        @Schema(description = "Existing ISO 4217 currency code", example = "JOD") String currencyCode,
        @Schema(description = "Sum in that currency", example = "125.5000") BigDecimal amount) {
}
