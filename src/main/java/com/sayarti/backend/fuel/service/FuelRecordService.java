package com.sayarti.backend.fuel.service;

import com.sayarti.backend.common.exception.BusinessValidationException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.fuel.dto.CreateFuelRecordRequest;
import com.sayarti.backend.fuel.dto.FuelRecordResponse;
import com.sayarti.backend.fuel.dto.UpdateFuelRecordRequest;
import com.sayarti.backend.fuel.entity.FuelRecord;
import com.sayarti.backend.fuel.repository.FuelRecordRepository;
import com.sayarti.backend.reference.repository.CurrencyRepository;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.vehicle.entity.PowertrainType;
import com.sayarti.backend.vehicle.entity.Vehicle;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuelRecordService {
    private static final int MONEY_SCALE = 4;
    private final FuelRecordRepository records;
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final CurrencyRepository currencies;

    public FuelRecordService(FuelRecordRepository records, VehicleRepository vehicles,
            UserRepository users, CurrencyRepository currencies) {
        this.records = records;
        this.vehicles = vehicles;
        this.users = users;
        this.currencies = currencies;
    }

    @Transactional
    public FuelRecordResponse create(AuthenticatedUser authenticated,
            UUID vehicleId, CreateFuelRecordRequest request) {
        Vehicle vehicle = ownedVehicle(authenticated, vehicleId);
        validateSupported(vehicle);
        long odometer = wholeOdometer(request.odometerKm());
        if (odometer < vehicle.getCurrentMileage()) {
            throw new BusinessValidationException(ErrorCode.INVALID_VEHICLE_MILEAGE,
                    "Fuel record odometer cannot be lower than vehicle mileage");
        }
        String currency = resolveCurrency(authenticated, request.currencyCode());
        BigDecimal total = total(request.quantityLiters(), request.pricePerLiter());
        FuelRecord record = new FuelRecord(vehicleId, request.odometerKm(),
                request.quantityLiters(), request.pricePerLiter(), total, currency,
                request.filledAt(), request.fullTank(), request.stationName(), request.notes());
        records.save(record);
        if (odometer > vehicle.getCurrentMileage()) {
            vehicle.updateMileage(odometer);
        }
        return FuelRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<FuelRecordResponse> list(AuthenticatedUser authenticated, UUID vehicleId) {
        ownedVehicle(authenticated, vehicleId);
        return records.findAllByVehicleIdAndDeletedAtIsNullOrderByFilledAtDescCreatedAtDesc(vehicleId)
                .stream().map(FuelRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FuelRecordResponse get(AuthenticatedUser authenticated, UUID vehicleId, UUID recordId) {
        ownedVehicle(authenticated, vehicleId);
        return FuelRecordResponse.from(activeRecord(vehicleId, recordId));
    }

    @Transactional
    public FuelRecordResponse update(AuthenticatedUser authenticated, UUID vehicleId, UUID recordId,
            UpdateFuelRecordRequest request) {
        ownedVehicle(authenticated, vehicleId);
        FuelRecord record = activeRecord(vehicleId, recordId);
        BigDecimal quantity = value(request.quantityLiters(), record.getQuantityLiters());
        BigDecimal price = value(request.pricePerLiter(), record.getPricePerLiter());
        String currency = request.currencyCode() == null ? record.getCurrencyCode()
                : validateCurrency(request.currencyCode());
        record.update(quantity, price, total(quantity, price), currency,
                value(request.filledAt(), record.getFilledAt()),
                value(request.fullTank(), record.isFullTank()),
                value(request.stationName(), record.getStationName()),
                value(request.notes(), record.getNotes()));
        return FuelRecordResponse.from(record);
    }

    @Transactional
    public void delete(AuthenticatedUser authenticated, UUID vehicleId, UUID recordId) {
        ownedVehicle(authenticated, vehicleId);
        activeRecord(vehicleId, recordId).delete();
    }

    private Vehicle ownedVehicle(AuthenticatedUser user, UUID id) {
        return vehicles.findByIdAndUserIdAndDeletedAtIsNull(id, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
    }
    private FuelRecord activeRecord(UUID vehicleId, UUID recordId) {
        return records.findByIdAndVehicleIdAndDeletedAtIsNull(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FUEL_RECORD_NOT_FOUND, "Fuel record not found"));
    }
    private void validateSupported(Vehicle vehicle) {
        if (vehicle.getPowertrainType() == PowertrainType.ELECTRIC) {
            throw new BusinessValidationException(ErrorCode.FUEL_NOT_SUPPORTED_FOR_VEHICLE,
                    "Fuel records are not supported for electric vehicles");
        }
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
                    "A default currency must be configured before recording fuel");
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
    private long wholeOdometer(BigDecimal odometer) {
        try {
            return odometer.longValueExact();
        } catch (ArithmeticException exception) {
            throw new BusinessValidationException(ErrorCode.INVALID_FUEL_RECORD,
                    "Odometer must be a whole number of kilometers");
        }
    }
    private BigDecimal total(BigDecimal quantity, BigDecimal price) {
        return quantity.multiply(price).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
    private <T> T value(T proposed, T existing) { return proposed == null ? existing : proposed; }
}
