package com.sayarti.backend.expense.repository;

import com.sayarti.backend.expense.entity.Expense;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findAllByVehicleIdAndDeletedAtIsNullOrderByExpenseDateDescCreatedAtDescIdDesc(
            UUID vehicleId);
    Optional<Expense> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);
    List<Expense> findAllByVehicleIdAndDeletedAtIsNull(UUID vehicleId);
    List<Expense> findByVehicleIdAndDeletedAtIsNullOrderByExpenseDateDescIdDesc(
            UUID vehicleId, Pageable pageable);
}
