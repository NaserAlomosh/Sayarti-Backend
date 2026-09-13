package com.sayarti.backend.expense.dto;

import com.sayarti.backend.expense.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateExpenseRequest(
        @NotNull ExpenseCategory category,
        @NotBlank @Size(max = 200) String title,
        @NotNull Instant expenseDate,
        @NotNull @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Size(max = 20) String currencyCode,
        @Size(max = 2000) String notes) { }
