package com.sayarti.backend.statistics.service;

import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.expense.entity.Expense;
import com.sayarti.backend.expense.entity.ExpenseCategory;
import com.sayarti.backend.expense.repository.ExpenseRepository;
import com.sayarti.backend.fuel.repository.FuelRecordRepository;
import com.sayarti.backend.fuel.service.FuelCalculator;
import com.sayarti.backend.maintenance.entity.MaintenanceCategory;
import com.sayarti.backend.maintenance.entity.MaintenanceRecord;
import com.sayarti.backend.maintenance.repository.MaintenanceRecordRepository;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.statistics.dto.CurrencyAverageResponse;
import com.sayarti.backend.statistics.dto.CurrencyRateResponse;
import com.sayarti.backend.statistics.dto.CurrencyTotalResponse;
import com.sayarti.backend.statistics.dto.ExpenseCategoryStatisticsResponse;
import com.sayarti.backend.statistics.dto.ExpenseStatisticsResponse;
import com.sayarti.backend.statistics.dto.FuelStatisticsResponse;
import com.sayarti.backend.statistics.dto.GeneralStatisticsResponse;
import com.sayarti.backend.statistics.dto.MaintenanceCategoryStatisticsResponse;
import com.sayarti.backend.statistics.dto.MaintenanceStatisticsResponse;
import com.sayarti.backend.vehicle.entity.Vehicle;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
    private final VehicleRepository vehicles;
    private final FuelRecordRepository fuelRecords;
    private final MaintenanceRecordRepository maintenanceRecords;
    private final ExpenseRepository expenses;
    private final ReminderRepository reminders;

    public StatisticsService(VehicleRepository vehicles, FuelRecordRepository fuelRecords,
            MaintenanceRecordRepository maintenanceRecords, ExpenseRepository expenses,
            ReminderRepository reminders) {
        this.vehicles = vehicles;
        this.fuelRecords = fuelRecords;
        this.maintenanceRecords = maintenanceRecords;
        this.expenses = expenses;
        this.reminders = reminders;
    }

    @Transactional(readOnly = true)
    public GeneralStatisticsResponse general(AuthenticatedUser user, UUID vehicleId) {
        Vehicle vehicle = vehicles.findByIdAndUserIdAndDeletedAtIsNull(vehicleId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
        var fuel = fuelRecords.findAllByVehicleIdAndDeletedAtIsNull(vehicleId);
        var maintenance = maintenanceRecords.findAllByVehicleIdAndDeletedAtIsNull(vehicleId);
        var expense = expenses.findAllByVehicleIdAndDeletedAtIsNull(vehicleId);
        var reminder = reminders.findAllByVehicleIdAndDeletedAtIsNull(vehicleId);

        // Reuse the established fuel calculation for quantity and currency cost totals.
        var fuelResult = FuelCalculator.calculate(fuel, YearMonth.now(ZoneOffset.UTC));
        var fuelCosts = fuelResult.costs().stream()
                .map(cost -> new CurrencyTotalResponse(cost.currencyCode(), cost.totalCost()))
                .toList();

        return new GeneralStatisticsResponse(vehicleId, vehicle.getCurrentMileage(), fuel.size(),
                fuelResult.totalQuantity(), fuelCosts, maintenance.size(),
                totals(maintenance, MaintenanceRecord::getCurrencyCode, MaintenanceRecord::getCost),
                expense.size(), totals(expense, Expense::getCurrencyCode, Expense::getAmount),
                reminder.stream().filter(value -> !value.isCompleted()).count(),
                reminder.stream().filter(value -> value.isCompleted()).count());
    }

    @Transactional(readOnly = true)
    public FuelStatisticsResponse fuel(AuthenticatedUser user, UUID vehicleId) {
        vehicles.findByIdAndUserIdAndDeletedAtIsNull(vehicleId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
        var records = fuelRecords.findAllByVehicleIdAndDeletedAtIsNull(vehicleId);
        var result = FuelCalculator.calculate(records, YearMonth.now(ZoneOffset.UTC));

        var costs = result.costs().stream()
                .map(cost -> new CurrencyTotalResponse(cost.currencyCode(), cost.totalCost()))
                .toList();
        var rates = result.costs().stream()
                .map(cost -> new CurrencyRateResponse(cost.currencyCode(), cost.costPerKm()))
                .toList();

        return new FuelStatisticsResponse(vehicleId, records.size(), result.totalQuantity(), costs,
                result.totalDistance(), result.averageKmPerLiter(),
                result.averageLitersPer100Km(), rates);
    }

    @Transactional(readOnly = true)
    public MaintenanceStatisticsResponse maintenance(AuthenticatedUser user, UUID vehicleId) {
        vehicles.findByIdAndUserIdAndDeletedAtIsNull(vehicleId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
        var records = maintenanceRecords
                .findAllByVehicleIdAndDeletedAtIsNullOrderByServiceDateDescCreatedAtDescIdDesc(
                        vehicleId);
        var latest = records.stream().findFirst().orElse(null);

        Map<MaintenanceCategory, List<MaintenanceRecord>> byCategory = new EnumMap<>(
                MaintenanceCategory.class);
        records.forEach(record -> byCategory.computeIfAbsent(record.getCategory(), ignored ->
                new java.util.ArrayList<>()).add(record));
        var categories = byCategory.entrySet().stream()
                .map(entry -> new MaintenanceCategoryStatisticsResponse(entry.getKey(),
                        entry.getValue().size(), totals(entry.getValue(),
                                MaintenanceRecord::getCurrencyCode, MaintenanceRecord::getCost)))
                .toList();

        return new MaintenanceStatisticsResponse(vehicleId, records.size(),
                totals(records, MaintenanceRecord::getCurrencyCode, MaintenanceRecord::getCost),
                averages(records), latest == null ? null : latest.getServiceDate(),
                latest == null ? null : latest.getMileageKm(), categories);
    }

    @Transactional(readOnly = true)
    public ExpenseStatisticsResponse expense(AuthenticatedUser user, UUID vehicleId) {
        vehicles.findByIdAndUserIdAndDeletedAtIsNull(vehicleId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
        var records = expenses
                .findAllByVehicleIdAndDeletedAtIsNullOrderByExpenseDateDescCreatedAtDescIdDesc(
                        vehicleId);
        var latest = records.stream().findFirst().orElse(null);

        Map<ExpenseCategory, List<Expense>> byCategory = new EnumMap<>(ExpenseCategory.class);
        records.forEach(record -> byCategory.computeIfAbsent(record.getCategory(), ignored ->
                new java.util.ArrayList<>()).add(record));
        var categories = byCategory.entrySet().stream()
                .map(entry -> new ExpenseCategoryStatisticsResponse(entry.getKey(),
                        entry.getValue().size(), totals(entry.getValue(),
                                Expense::getCurrencyCode, Expense::getAmount)))
                .toList();

        return new ExpenseStatisticsResponse(vehicleId, records.size(),
                totals(records, Expense::getCurrencyCode, Expense::getAmount),
                averages(records, Expense::getCurrencyCode, Expense::getAmount), categories,
                latest == null ? null : latest.getExpenseDate());
    }

    private List<CurrencyAverageResponse> averages(List<MaintenanceRecord> records) {
        return averages(records, MaintenanceRecord::getCurrencyCode, MaintenanceRecord::getCost);
    }

    private <T> List<CurrencyAverageResponse> averages(List<T> records,
            Function<T, String> currency, Function<T, BigDecimal> amount) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        Map<String, Long> counts = new TreeMap<>();
        records.forEach(record -> {
            totals.merge(currency.apply(record), amount.apply(record), BigDecimal::add);
            counts.merge(currency.apply(record), 1L, Long::sum);
        });
        return totals.entrySet().stream().map(entry -> new CurrencyAverageResponse(entry.getKey(),
                entry.getValue().divide(BigDecimal.valueOf(counts.get(entry.getKey())), 4,
                        RoundingMode.HALF_UP))).toList();
    }

    private <T> List<CurrencyTotalResponse> totals(List<T> records,
            Function<T, String> currency, Function<T, BigDecimal> amount) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        records.forEach(record -> totals.merge(currency.apply(record), amount.apply(record),
                BigDecimal::add));
        return totals.entrySet().stream().map(entry -> new CurrencyTotalResponse(entry.getKey(),
                entry.getValue().setScale(4, RoundingMode.HALF_UP))).toList();
    }
}
