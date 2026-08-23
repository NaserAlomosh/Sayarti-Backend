package com.sayarti.backend.statistics.dto;

import com.sayarti.backend.maintenance.entity.MaintenanceCategory;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Maintenance record count and original-currency costs for one category")
public record MaintenanceCategoryStatisticsResponse(
        @Schema(description = "Maintenance category", example = "OIL_CHANGE")
        MaintenanceCategory category,
        @Schema(description = "Number of active records in this category", example = "2")
        long totalMaintenanceRecords,
        @ArraySchema(arraySchema = @Schema(description = "Category costs separated by currency"))
        List<CurrencyTotalResponse> totalMaintenanceCostByCurrency) {
}
