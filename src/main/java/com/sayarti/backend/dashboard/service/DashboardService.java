package com.sayarti.backend.dashboard.service;

import com.sayarti.backend.dashboard.dto.DashboardResponse;
import com.sayarti.backend.reminder.dto.ReminderResponse;
import com.sayarti.backend.reminder.entity.Reminder;
import com.sayarti.backend.reminder.entity.ReminderTriggerType;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.statistics.service.StatisticsService;
import com.sayarti.backend.vehicle.service.VehicleService;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final VehicleService vehicleService;
    private final StatisticsService statistics;
    private final ReminderRepository reminders;
    private final Clock clock;

    public DashboardService(VehicleService vehicleService, StatisticsService statistics,
            ReminderRepository reminders) {
        this(vehicleService, statistics, reminders, Clock.systemUTC());
    }

    DashboardService(VehicleService vehicleService, StatisticsService statistics,
            ReminderRepository reminders, Clock clock) {
        this.vehicleService = vehicleService;
        this.statistics = statistics;
        this.reminders = reminders;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(AuthenticatedUser user, UUID vehicleId) {
        // This lookup is deliberately first: it provides the established resource-hiding check.
        var vehicle = vehicleService.get(user, vehicleId);
        var general = statistics.general(user, vehicleId);
        var fuel = statistics.fuel(user, vehicleId);
        var maintenance = statistics.maintenance(user, vehicleId);
        var expenses = statistics.expense(user, vehicleId);
        var totalCost = statistics.totalCost(user, vehicleId);
        var activeReminders = reminders
                .findAllByVehicleIdAndCompletedFalseAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                        vehicleId);

        ReminderResponse upcoming = nearestUpcoming(activeReminders, vehicle.currentMileage());
        return new DashboardResponse(vehicleId, vehicle, vehicle.currentMileage(),
                new DashboardResponse.FuelSummary(fuel.totalFuelRecords(),
                        fuel.totalFuelQuantity(), fuel.totalFuelCostByCurrency(),
                        fuel.averageFuelEfficiencyKmPerLiter(),
                        fuel.averageFuelConsumptionLitersPer100Km()),
                new DashboardResponse.MaintenanceSummary(maintenance.totalMaintenanceRecords(),
                        maintenance.latestMaintenanceDate(),
                        maintenance.totalMaintenanceCostByCurrency()),
                new DashboardResponse.ExpenseSummary(expenses.totalExpenseRecords(),
                        expenses.totalExpenseAmountByCurrency()),
                new DashboardResponse.TotalCostSummary(totalCost.totalVehicleCostByCurrency(),
                        totalCost.averageMonthlyCostByCurrency(),
                        totalCost.costPerKilometerByCurrency()),
                new DashboardResponse.ReminderSummary(general.activeReminderCount(),
                        general.completedReminderCount(), upcoming));
    }

    private ReminderResponse nearestUpcoming(java.util.List<Reminder> active, long mileage) {
        Instant now = clock.instant();
        return active.stream()
                .filter(reminder -> reminder.getTriggerType() == ReminderTriggerType.DATE
                        && reminder.getTargetDate().isAfter(now))
                .min(Comparator.comparing(Reminder::getTargetDate)
                        .thenComparing(Reminder::getId))
                .or(() -> active.stream()
                        .filter(reminder -> reminder.getTriggerType() == ReminderTriggerType.MILEAGE
                                && reminder.getTargetMileage() > mileage)
                        .min(Comparator.comparing(Reminder::getTargetMileage)
                                .thenComparing(Reminder::getId)))
                .map(ReminderResponse::from).orElse(null);
    }
}
