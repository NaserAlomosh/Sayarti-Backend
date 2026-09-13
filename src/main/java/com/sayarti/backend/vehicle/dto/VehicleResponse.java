package com.sayarti.backend.vehicle.dto;

import com.sayarti.backend.vehicle.entity.FuelType;
import com.sayarti.backend.vehicle.entity.PowertrainType;
import com.sayarti.backend.vehicle.entity.Vehicle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
        UUID id, String brand, String model, int year, PowertrainType powertrainType,
        long currentMileage, String licensePlate, String nickname, String imageUrl,
        FuelType fuelType, BigDecimal fuelTankCapacityLiters, BigDecimal batteryCapacityKwh,
        BigDecimal estimatedRangeKm, Instant createdAt, Instant updatedAt) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getBrand(), vehicle.getModel(),
                vehicle.getYear(), vehicle.getPowertrainType(), vehicle.getCurrentMileage(),
                vehicle.getLicensePlate(), vehicle.getNickname(), vehicle.getImageUrl(),
                vehicle.getFuelType(), vehicle.getFuelTankCapacityLiters(),
                vehicle.getBatteryCapacityKwh(), vehicle.getEstimatedRangeKm(),
                vehicle.getCreatedAt(), vehicle.getUpdatedAt());
    }
}
