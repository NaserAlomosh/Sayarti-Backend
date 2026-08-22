package com.sayarti.backend.maintenance.dto;

import com.sayarti.backend.maintenance.entity.MaintenanceCategory;
import com.sayarti.backend.maintenance.entity.MaintenanceRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MaintenanceRecordResponse(UUID id, UUID vehicleId, MaintenanceCategory category,
        String title, Instant serviceDate, BigDecimal mileageKm, BigDecimal cost,
        String currencyCode, String serviceProvider, String notes, Instant createdAt,
        Instant updatedAt) {
    public static MaintenanceRecordResponse from(MaintenanceRecord record) {
        return new MaintenanceRecordResponse(record.getId(), record.getVehicleId(),
                record.getCategory(), record.getTitle(), record.getServiceDate(),
                record.getMileageKm(), record.getCost(), record.getCurrencyCode(),
                record.getServiceProvider(), record.getNotes(), record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
