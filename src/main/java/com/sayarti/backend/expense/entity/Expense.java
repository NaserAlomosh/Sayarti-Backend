package com.sayarti.backend.expense.entity;

import com.sayarti.backend.common.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "expenses")
public class Expense extends BaseAuditableEntity {
    @Id private UUID id;
    @Column(name = "vehicle_id", nullable = false) private UUID vehicleId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ExpenseCategory category;
    @Nationalized @Column(nullable = false, length = 200) private String title;
    @Column(name = "expense_date", nullable = false) private Instant expenseDate;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode;
    @Nationalized @Column(length = 2000) private String notes;
    @Column(name = "deleted_at") private Instant deletedAt;

    protected Expense() { }

    public Expense(UUID vehicleId, ExpenseCategory category, String title, Instant expenseDate,
            BigDecimal amount, String currencyCode, String notes) {
        this.id = UUID.randomUUID();
        this.vehicleId = vehicleId;
        update(category, title, expenseDate, amount, currencyCode, notes);
    }

    public void update(ExpenseCategory category, String title, Instant expenseDate,
            BigDecimal amount, String currencyCode, String notes) {
        this.category = category;
        this.title = title.trim();
        this.expenseDate = expenseDate;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.notes = notes == null ? null : notes.trim();
    }

    public void delete() { deletedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getVehicleId() { return vehicleId; }
    public ExpenseCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public Instant getExpenseDate() { return expenseDate; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }
    public String getNotes() { return notes; }
    public Instant getDeletedAt() { return deletedAt; }
}
