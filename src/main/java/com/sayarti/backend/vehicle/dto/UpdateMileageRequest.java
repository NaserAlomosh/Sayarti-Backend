package com.sayarti.backend.vehicle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateMileageRequest(@NotNull @Min(0) Long mileage) {
}
