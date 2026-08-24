package com.sayarti.backend.activity.service;

import com.sayarti.backend.activity.dto.ActivityType;
import com.sayarti.backend.activity.dto.VehicleActivityResponse;
import com.sayarti.backend.expense.repository.ExpenseRepository;
import com.sayarti.backend.fuel.repository.FuelRecordRepository;
import com.sayarti.backend.maintenance.repository.MaintenanceRecordRepository;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.vehicle.service.VehicleService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleActivityService {
    private final VehicleService vehicleService;
    private final FuelRecordRepository fuel;
    private final MaintenanceRecordRepository maintenance;
    private final ExpenseRepository expenses;
    private final ReminderRepository reminders;

    public VehicleActivityService(VehicleService vehicleService, FuelRecordRepository fuel,
            MaintenanceRecordRepository maintenance, ExpenseRepository expenses,
            ReminderRepository reminders) {
        this.vehicleService = vehicleService;
        this.fuel = fuel;
        this.maintenance = maintenance;
        this.expenses = expenses;
        this.reminders = reminders;
    }

    @Transactional(readOnly = true)
    public List<VehicleActivityResponse> recent(
            AuthenticatedUser user, UUID vehicleId, int limit) {
        // Reuse the established resource-hiding ownership check before querying activity.
        vehicleService.get(user, vehicleId);
        var page = PageRequest.of(0, limit);
        List<VehicleActivityResponse> activity = new ArrayList<>(limit * 4);
        fuel.findByVehicleIdAndDeletedAtIsNullOrderByFilledAtDescIdDesc(vehicleId, page)
                .forEach(record -> activity.add(new VehicleActivityResponse(ActivityType.FUEL,
                        record.getId(), vehicleId, "Fuel refill", record.getFilledAt(),
                        record.getTotalCost(), record.getCurrencyCode())));
        maintenance.findByVehicleIdAndDeletedAtIsNullOrderByServiceDateDescIdDesc(vehicleId, page)
                .forEach(record -> activity.add(new VehicleActivityResponse(
                        ActivityType.MAINTENANCE, record.getId(), vehicleId, record.getTitle(),
                        record.getServiceDate(), record.getCost(), record.getCurrencyCode())));
        expenses.findByVehicleIdAndDeletedAtIsNullOrderByExpenseDateDescIdDesc(vehicleId, page)
                .forEach(record -> activity.add(new VehicleActivityResponse(ActivityType.EXPENSE,
                        record.getId(), vehicleId, record.getTitle(), record.getExpenseDate(),
                        record.getAmount(), record.getCurrencyCode())));
        reminders.findByVehicleIdAndCompletedTrueAndCompletedAtIsNotNullAndDeletedAtIsNullOrderByCompletedAtDescIdDesc(
                        vehicleId, page)
                .forEach(reminder -> activity.add(new VehicleActivityResponse(
                        ActivityType.REMINDER, reminder.getId(), vehicleId, reminder.getTitle(),
                        reminder.getCompletedAt(), null, null)));
        return activity.stream().sorted(Comparator
                        .comparing(VehicleActivityResponse::occurredAt).reversed()
                        .thenComparing(item -> item.type().name())
                        .thenComparing(VehicleActivityResponse::referenceId))
                .limit(limit).toList();
    }
}
