package com.sayarti.backend.maintenance.dto;

import com.sayarti.backend.maintenance.entity.MaintenanceCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record UpdateMaintenanceRecordRequest(
        MaintenanceCategory category,
        @Size(min = 1, max = 200) String title,
        Instant serviceDate,
        @DecimalMin("0.0") @Digits(integer = 17, fraction = 2) BigDecimal mileageKm,
        @DecimalMin("0.0") @Digits(integer = 15, fraction = 4) BigDecimal cost,
        @Size(min = 3, max = 3) String currencyCode,
        @Size(max = 200) String serviceProvider,
        @Size(max = 2000) String notes) { }
