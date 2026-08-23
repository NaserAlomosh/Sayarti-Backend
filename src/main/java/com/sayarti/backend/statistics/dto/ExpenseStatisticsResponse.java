package com.sayarti.backend.statistics.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Expense statistics for one active, owned vehicle. An empty expense history "
        + "returns zero records, empty amount and category arrays, and a null latest date.")
public record ExpenseStatisticsResponse(
        @Schema(description = "Vehicle identifier") UUID vehicleId,
        @Schema(description = "Number of active expense records", example = "3")
        long totalExpenseRecords,
        @ArraySchema(arraySchema = @Schema(description = "Expense totals separated by original currency"))
        List<CurrencyTotalResponse> totalExpenseAmountByCurrency,
        @ArraySchema(arraySchema = @Schema(description = "Average expense amounts separated by original currency"))
        List<CurrencyAverageResponse> averageExpenseAmountByCurrency,
        @ArraySchema(arraySchema = @Schema(description = "Categories in enum declaration order; "
                + "categories without records are omitted"))
        List<ExpenseCategoryStatisticsResponse> expenseByCategory,
        @Schema(description = "Date of the latest active expense record", nullable = true,
                example = "2026-08-20T10:00:00Z")
        Instant latestExpenseDate) {
}
