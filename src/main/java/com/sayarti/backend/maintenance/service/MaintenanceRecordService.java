package com.sayarti.backend.maintenance.service;

import com.sayarti.backend.common.exception.BusinessValidationException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.maintenance.dto.CreateMaintenanceRecordRequest;
import com.sayarti.backend.maintenance.dto.MaintenanceRecordResponse;
import com.sayarti.backend.maintenance.dto.UpdateMaintenanceRecordRequest;
import com.sayarti.backend.maintenance.entity.MaintenanceRecord;
import com.sayarti.backend.maintenance.repository.MaintenanceRecordRepository;
import com.sayarti.backend.reference.repository.CurrencyRepository;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.vehicle.entity.Vehicle;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceRecordService {
    private final MaintenanceRecordRepository records;
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final CurrencyRepository currencies;

    public MaintenanceRecordService(MaintenanceRecordRepository records,
            VehicleRepository vehicles, UserRepository users, CurrencyRepository currencies) {
        this.records = records;
        this.vehicles = vehicles;
        this.users = users;
        this.currencies = currencies;
    }

    @Transactional
    public MaintenanceRecordResponse create(AuthenticatedUser user, UUID vehicleId,
            CreateMaintenanceRecordRequest request) {
        Vehicle vehicle = ownedVehicle(user, vehicleId);
        String currency = resolveCurrency(user, request.currencyCode());
        MaintenanceRecord record = new MaintenanceRecord(vehicleId, request.category(),
                request.title(), request.serviceDate(), request.mileageKm(), request.cost(),
                currency, request.serviceProvider(), request.notes());
        records.save(record);
        updateVehicleMileageIfHigher(vehicle, request.mileageKm());
        return MaintenanceRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> list(AuthenticatedUser user, UUID vehicleId) {
        ownedVehicle(user, vehicleId);
        return records
                .findAllByVehicleIdAndDeletedAtIsNullOrderByServiceDateDescCreatedAtDescIdDesc(
                        vehicleId)
                .stream().map(MaintenanceRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceRecordResponse get(AuthenticatedUser user, UUID vehicleId, UUID recordId) {
        ownedVehicle(user, vehicleId);
        return MaintenanceRecordResponse.from(activeRecord(vehicleId, recordId));
    }

    @Transactional
    public MaintenanceRecordResponse update(AuthenticatedUser user, UUID vehicleId, UUID recordId,
            UpdateMaintenanceRecordRequest request) {
        Vehicle vehicle = ownedVehicle(user, vehicleId);
        MaintenanceRecord record = activeRecord(vehicleId, recordId);
        BigDecimal mileage = value(request.mileageKm(), record.getMileageKm());
        String title = value(request.title(), record.getTitle());
        if (title.isBlank()) {
            throw new BusinessValidationException(ErrorCode.INVALID_MAINTENANCE_RECORD,
                    "Maintenance title cannot be blank");
        }
        String currency = request.currencyCode() == null ? record.getCurrencyCode()
                : validateCurrency(request.currencyCode());
        record.update(value(request.category(), record.getCategory()), title,
                value(request.serviceDate(), record.getServiceDate()), mileage,
                value(request.cost(), record.getCost()), currency,
                value(request.serviceProvider(), record.getServiceProvider()),
                value(request.notes(), record.getNotes()));
        updateVehicleMileageIfHigher(vehicle, mileage);
        return MaintenanceRecordResponse.from(record);
    }

    @Transactional
    public void delete(AuthenticatedUser user, UUID vehicleId, UUID recordId) {
        ownedVehicle(user, vehicleId);
        activeRecord(vehicleId, recordId).delete();
    }

    private Vehicle ownedVehicle(AuthenticatedUser user, UUID vehicleId) {
        return vehicles.findByIdAndUserIdAndDeletedAtIsNull(vehicleId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
    }

    private MaintenanceRecord activeRecord(UUID vehicleId, UUID recordId) {
        return records.findByIdAndVehicleIdAndDeletedAtIsNull(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.MAINTENANCE_NOT_FOUND, "Maintenance record not found"));
    }

    private String resolveCurrency(AuthenticatedUser authenticated, String requested) {
        if (requested != null) {
            return validateCurrency(requested);
        }
        User user = users.findByIdAndDeletedAtIsNull(authenticated.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND, "User not found"));
        if (user.getDefaultCurrencyCode() == null) {
            throw new BusinessValidationException(ErrorCode.COUNTRY_SETUP_INCOMPLETE,
                    "A default currency must be configured before recording maintenance");
        }
        return validateCurrency(user.getDefaultCurrencyCode());
    }

    private String validateCurrency(String candidate) {
        String code = candidate.trim().toUpperCase(Locale.ROOT);
        currencies.findByCodeAndActiveTrue(code).orElseThrow(() ->
                new BusinessValidationException(ErrorCode.CURRENCY_NOT_SUPPORTED,
                        "Currency is not supported"));
        return code;
    }

    private void updateVehicleMileageIfHigher(Vehicle vehicle, BigDecimal mileage) {
        try {
            long wholeMileage = mileage.longValueExact();
            if (wholeMileage > vehicle.getCurrentMileage()) {
                vehicle.updateMileage(wholeMileage);
            }
        } catch (ArithmeticException exception) {
            throw new BusinessValidationException(ErrorCode.INVALID_MAINTENANCE_RECORD,
                    "Maintenance mileage must be a whole number of kilometers");
        }
    }

    private <T> T value(T proposed, T existing) { return proposed == null ? existing : proposed; }
}
