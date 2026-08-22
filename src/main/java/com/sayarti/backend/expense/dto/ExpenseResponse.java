package com.sayarti.backend.expense.dto;

import com.sayarti.backend.expense.entity.Expense;
import com.sayarti.backend.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExpenseResponse(UUID id, UUID vehicleId, ExpenseCategory category, String title,
        Instant expenseDate, BigDecimal amount, String currencyCode, String notes,
        Instant createdAt, Instant updatedAt, Instant deletedAt) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getVehicleId(), expense.getCategory(),
                expense.getTitle(), expense.getExpenseDate(), expense.getAmount(),
                expense.getCurrencyCode(), expense.getNotes(), expense.getCreatedAt(),
                expense.getUpdatedAt(), expense.getDeletedAt());
    }
}
