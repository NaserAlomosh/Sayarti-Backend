package com.sayarti.backend.vehicle.dto;

import com.sayarti.backend.vehicle.entity.FuelType;
import com.sayarti.backend.vehicle.entity.PowertrainType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateVehicleRequest(
        @NotBlank @Size(max = 100) String brand,
        @NotBlank @Size(max = 100) String model,
        @NotNull @Min(1886) Integer year,
        @NotNull PowertrainType powertrainType,
        @NotNull @Min(0) Long currentMileage,
        @Size(max = 30) String licensePlate,
        @Size(max = 100) String nickname,
        @Size(max = 2048) String imageUrl,
        FuelType fuelType,
        @Positive BigDecimal fuelTankCapacityLiters,
        @Positive BigDecimal batteryCapacityKwh,
        @Positive BigDecimal estimatedRangeKm) {
}
