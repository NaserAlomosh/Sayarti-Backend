package com.sayarti.backend.fuel.dto;

import com.sayarti.backend.fuel.entity.FuelRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FuelRecordResponse(UUID id, UUID vehicleId, BigDecimal odometerKm,
        BigDecimal quantityLiters, BigDecimal pricePerLiter, BigDecimal totalCost,
        String currencyCode, Instant filledAt, boolean fullTank, String stationName, String notes,
        Instant createdAt, Instant updatedAt) {
    public static FuelRecordResponse from(FuelRecord record) {
        return new FuelRecordResponse(record.getId(), record.getVehicleId(), record.getOdometerKm(),
                record.getQuantityLiters(), record.getPricePerLiter(), record.getTotalCost(),
                record.getCurrencyCode(), record.getFilledAt(), record.isFullTank(),
                record.getStationName(), record.getNotes(), record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
