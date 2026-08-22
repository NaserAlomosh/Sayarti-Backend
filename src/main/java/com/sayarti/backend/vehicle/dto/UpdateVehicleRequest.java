package com.sayarti.backend.vehicle.dto;

import com.sayarti.backend.vehicle.entity.FuelType;
import com.sayarti.backend.vehicle.entity.PowertrainType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateVehicleRequest(
        @Size(min = 1, max = 100) String brand,
        @Size(min = 1, max = 100) String model,
        @Min(1886) Integer year,
        PowertrainType powertrainType,
        @Size(max = 30) String licensePlate,
        @Size(max = 100) String nickname,
        @Size(max = 2048) String imageUrl,
        FuelType fuelType,
        @Positive BigDecimal fuelTankCapacityLiters,
        @Positive BigDecimal batteryCapacityKwh,
        @Positive BigDecimal estimatedRangeKm) {
}
