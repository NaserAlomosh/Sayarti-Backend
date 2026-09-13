package com.sayarti.backend.expense.dto;

import com.sayarti.backend.expense.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record UpdateExpenseRequest(
        ExpenseCategory category,
        @Size(max = 200) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String title,
        Instant expenseDate,
        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Size(max = 20) String currencyCode,
        @Size(max = 2000) String notes) { }
