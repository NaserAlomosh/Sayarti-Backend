package com.sayarti.backend.fuel.service;

import com.sayarti.backend.common.exception.BusinessValidationException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.common.query.ListQuerySupport;
import com.sayarti.backend.common.response.PageResponse;
import com.sayarti.backend.fuel.dto.CreateFuelRecordRequest;
import com.sayarti.backend.fuel.dto.FuelCostSummary;
import com.sayarti.backend.fuel.dto.FuelRecordResponse;
import com.sayarti.backend.fuel.dto.FuelSummaryResponse;
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
import java.time.Instant;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuelRecordService {
    private static final int MONEY_SCALE = 4;
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "filledAt", "filledAt", "createdAt", "createdAt", "odometerKm", "odometerKm", "totalCost", "totalCost");
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
    public PageResponse<FuelRecordResponse> list(AuthenticatedUser authenticated, UUID vehicleId, int page, int size,
            String sortBy, String sortDirection, Instant from, Instant to) {
        ownedVehicle(authenticated, vehicleId);
        ListQuerySupport.validateRange(from, to);
        var pageable = ListQuerySupport.pageable(page, size, sortBy, sortDirection,
                "filledAt", Sort.Direction.DESC, SORT_FIELDS, "createdAt");
        var result = records.findAll((root, query, cb) -> {
            var predicate = cb.and(cb.equal(root.get("vehicleId"), vehicleId),
                    cb.isNull(root.get("deletedAt")));
            if (from != null) predicate = cb.and(predicate, cb.greaterThanOrEqualTo(
                    root.<Instant>get("filledAt"), from));
            if (to != null) predicate = cb.and(predicate, cb.lessThanOrEqualTo(
                    root.<Instant>get("filledAt"), to));
            return predicate;
        }, pageable).map(FuelRecordResponse::from);
        return PageResponse.from(result);
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

    @Transactional(readOnly = true)
    public FuelSummaryResponse summary(AuthenticatedUser authenticated, UUID vehicleId,
            YearMonth month) {
        Vehicle vehicle = ownedVehicle(authenticated, vehicleId);
        boolean supported = vehicle.getPowertrainType() != PowertrainType.ELECTRIC;
        FuelCalculator.Result result = FuelCalculator.calculate(
                records.findAllByVehicleIdAndDeletedAtIsNull(vehicleId), month);
        List<FuelCostSummary> costs = result.costs().stream()
                .map(cost -> new FuelCostSummary(cost.currencyCode(), cost.totalCost(),
                        cost.monthlyCost(), supported ? cost.costPerKm() : null))
                .toList();
        return new FuelSummaryResponse(vehicleId, supported, month, result.totalQuantity(),
                result.totalDistance(), supported ? result.averageKmPerLiter() : null,
                supported ? result.averageLitersPer100Km() : null, costs);
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
