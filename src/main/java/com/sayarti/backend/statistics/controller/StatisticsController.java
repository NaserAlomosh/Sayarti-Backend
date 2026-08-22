package com.sayarti.backend.statistics.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.statistics.dto.GeneralStatisticsResponse;
import com.sayarti.backend.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/statistics")
@Tag(name = "Statistics", description = "Authenticated vehicle-level V1 general statistics")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {
    private final StatisticsService service;

    public StatisticsController(StatisticsService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get general vehicle statistics", description = "Aggregates only "
            + "non-deleted V1 records. Monetary totals retain their original currency and are "
            + "grouped by currency code; no conversion occurs. Empty domains return zero counts, "
            + "zero fuel quantity, and empty total arrays. Unknown, deleted, and other users' "
            + "vehicles all return VEHICLE_NOT_FOUND, hiding resource existence.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "General statistics response",
            content = @Content(schema = @Schema(implementation = GeneralStatisticsResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Bearer authentication is missing or invalid")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "VEHICLE_NOT_FOUND for missing, deleted, or inaccessible vehicles")
    public ApiResponse<GeneralStatisticsResponse> get(
            @Parameter(description = "Owned active vehicle UUID", required = true)
            @PathVariable UUID vehicleId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.general(user, vehicleId));
    }
}
