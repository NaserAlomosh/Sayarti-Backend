package com.sayarti.backend.dashboard.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.dashboard.dto.DashboardResponse;
import com.sayarti.backend.dashboard.service.DashboardService;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
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
@RequestMapping("/api/v1/vehicles/{vehicleId}/dashboard")
@Tag(name = "Dashboard", description = "Authenticated V1 vehicle overview")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get a vehicle dashboard", description = "Returns concise vehicle, fuel, "
            + "maintenance, expense, true-cost, and reminder summaries using only non-soft-deleted "
            + "records. Money is grouped by original currency and is never converted. Empty data "
            + "returns stable zeros, empty arrays, and null latest/upcoming fields. Fuel efficiency "
            + "and per-kilometer amounts are null when positive consecutive odometer history is "
            + "insufficient. The upcoming reminder is the earliest future date reminder, falling "
            + "back to the nearest mileage threshold. Missing, deleted, and cross-user vehicles "
            + "all return VEHICLE_NOT_FOUND, without revealing existence.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Dashboard response, including stable empty and multi-currency results",
            content = @Content(schema = @Schema(implementation = DashboardResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Bearer authentication is missing or invalid")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "VEHICLE_NOT_FOUND for missing, deleted, or inaccessible vehicles")
    public ApiResponse<DashboardResponse> get(
            @Parameter(description = "Owned active vehicle UUID", required = true)
            @PathVariable UUID vehicleId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.get(user, vehicleId));
    }
}
