package com.sayarti.backend.statistics.dto;

import com.sayarti.backend.expense.entity.ExpenseCategory;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Expense record count and original-currency amounts for one category")
public record ExpenseCategoryStatisticsResponse(
        @Schema(description = "Expense category", example = "INSURANCE")
        ExpenseCategory category,
        @Schema(description = "Number of active records in this category", example = "2")
        long recordCount,
        @ArraySchema(arraySchema = @Schema(description = "Category amounts separated by currency"))
        List<CurrencyTotalResponse> totalAmountByCurrency) {
}
