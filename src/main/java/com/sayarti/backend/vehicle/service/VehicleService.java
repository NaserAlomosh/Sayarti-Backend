package com.sayarti.backend.vehicle.service;

import com.sayarti.backend.common.exception.BusinessValidationException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.vehicle.dto.CreateVehicleRequest;
import com.sayarti.backend.vehicle.dto.UpdateVehicleRequest;
import com.sayarti.backend.vehicle.dto.VehicleResponse;
import com.sayarti.backend.vehicle.entity.FuelType;
import com.sayarti.backend.vehicle.entity.PowertrainType;
import com.sayarti.backend.vehicle.entity.Vehicle;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {
    private static final EnumSet<FuelType> GASOLINE_FUELS = EnumSet.of(FuelType.GASOLINE_90,
            FuelType.GASOLINE_95, FuelType.GASOLINE_98, FuelType.OTHER);
    private final VehicleRepository vehicles;

    public VehicleService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    @Transactional
    public VehicleResponse create(AuthenticatedUser user, CreateVehicleRequest request) {
        validateYear(request.year());
        validateConfiguration(request.powertrainType(), request.fuelType(),
                request.fuelTankCapacityLiters(), request.batteryCapacityKwh(),
                request.estimatedRangeKm());
        Vehicle vehicle = new Vehicle(user.id(), request.brand(), request.model(), request.year(),
                request.powertrainType(), request.currentMileage(), request.licensePlate(),
                request.nickname(), request.imageUrl(), request.fuelType(),
                request.fuelTankCapacityLiters(), request.batteryCapacityKwh(),
                request.estimatedRangeKm());
        return VehicleResponse.from(vehicles.save(vehicle));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> list(AuthenticatedUser user) {
        return vehicles.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.id()).stream()
                .map(VehicleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(AuthenticatedUser user, UUID id) {
        return VehicleResponse.from(owned(user, id));
    }

    @Transactional
    public VehicleResponse update(AuthenticatedUser user, UUID id, UpdateVehicleRequest request) {
        Vehicle vehicle = owned(user, id);
        String brand = request.brand() == null ? vehicle.getBrand() : request.brand();
        String model = request.model() == null ? vehicle.getModel() : request.model();
        if (brand.isBlank() || model.isBlank()) {
            invalid("Vehicle brand and model cannot be blank");
        }
        int year = request.year() == null ? vehicle.getYear() : request.year();
        PowertrainType powertrain = request.powertrainType() == null
                ? vehicle.getPowertrainType() : request.powertrainType();
        boolean changingPowertrain = request.powertrainType() != null;
        FuelType fuel = changingPowertrain ? request.fuelType()
                : value(request.fuelType(), vehicle.getFuelType());
        BigDecimal tank = changingPowertrain ? request.fuelTankCapacityLiters()
                : value(request.fuelTankCapacityLiters(), vehicle.getFuelTankCapacityLiters());
        BigDecimal battery = changingPowertrain ? request.batteryCapacityKwh()
                : value(request.batteryCapacityKwh(), vehicle.getBatteryCapacityKwh());
        BigDecimal range = changingPowertrain ? request.estimatedRangeKm()
                : value(request.estimatedRangeKm(), vehicle.getEstimatedRangeKm());
        validateYear(year);
        validateConfiguration(powertrain, fuel, tank, battery, range);
        vehicle.updateDetails(brand, model, year, powertrain,
                value(request.licensePlate(), vehicle.getLicensePlate()),
                value(request.nickname(), vehicle.getNickname()),
                value(request.imageUrl(), vehicle.getImageUrl()), fuel, tank, battery, range);
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public VehicleResponse updateMileage(AuthenticatedUser user, UUID id, long mileage) {
        Vehicle vehicle = owned(user, id);
        if (mileage < vehicle.getCurrentMileage()) {
            throw new BusinessValidationException(ErrorCode.INVALID_VEHICLE_MILEAGE,
                    "Vehicle mileage cannot decrease");
        }
        vehicle.updateMileage(mileage);
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public void delete(AuthenticatedUser user, UUID id) {
        owned(user, id).delete();
    }

    private Vehicle owned(AuthenticatedUser user, UUID id) {
        return vehicles.findByIdAndUserIdAndDeletedAtIsNull(id, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
    }

    private void validateYear(int year) {
        if (year > Year.now().getValue() + 1) {
            invalid("Vehicle year cannot exceed next year");
        }
    }

    private void validateConfiguration(PowertrainType type, FuelType fuel, BigDecimal tank,
            BigDecimal battery, BigDecimal range) {
        switch (type) {
            case GASOLINE -> {
                if (fuel == null || !GASOLINE_FUELS.contains(fuel)) {
                    invalid("Gasoline vehicles require a gasoline fuel type");
                }
                if (battery != null) {
                    invalid("Gasoline vehicles cannot have battery capacity");
                }
            }
            case DIESEL -> {
                if (fuel != FuelType.DIESEL) {
                    invalid("Diesel vehicles require DIESEL fuel type");
                }
                if (battery != null) {
                    invalid("Diesel vehicles cannot have battery capacity");
                }
            }
            case HYBRID -> {
                if (fuel == null || !GASOLINE_FUELS.contains(fuel)) {
                    invalid("Hybrid vehicles require a gasoline fuel type");
                }
            }
            case PLUG_IN_HYBRID -> {
                if (fuel == null || !GASOLINE_FUELS.contains(fuel)) {
                    invalid("Plug-in hybrid vehicles require a gasoline fuel type");
                }
                if (battery == null) {
                    invalid("Plug-in hybrid vehicles require battery capacity");
                }
            }
            case ELECTRIC -> {
                if (fuel != null) {
                    invalid("Electric vehicles cannot have a fuel type");
                }
                if (tank != null) {
                    invalid("Electric vehicles cannot have a fuel tank capacity");
                }
                if (battery == null) {
                    invalid("Electric vehicles require positive battery capacity");
                }
                if (range == null) {
                    invalid("Electric vehicles require positive estimated range");
                }
            }
        }
    }
    private void invalid(String message) {
        throw new BusinessValidationException(
                ErrorCode.INVALID_VEHICLE_CONFIGURATION, message);
    }
    private <T> T value(T proposed, T existing) {
        return proposed == null ? existing : proposed;
    }
}
