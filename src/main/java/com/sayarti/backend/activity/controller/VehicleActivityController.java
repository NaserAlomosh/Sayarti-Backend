package com.sayarti.backend.activity.controller;

import com.sayarti.backend.activity.dto.VehicleActivityResponse;
import com.sayarti.backend.activity.service.VehicleActivityService;
import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/activity")
@Tag(name = "Vehicle Activity", description = "Unified recent vehicle timeline")
@SecurityRequirement(name = "bearerAuth")
public class VehicleActivityController {
    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;
    private final VehicleActivityService service;

    public VehicleActivityController(VehicleActivityService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get recent vehicle activity", description = "Returns a bounded, "
            + "newest-first feed derived from active fuel, maintenance, and expense records and "
            + "completed reminders. Chronology uses each domain's business event timestamp. "
            + "Missing, deleted, and inaccessible vehicles return VEHICLE_NOT_FOUND.")
    public ApiResponse<List<VehicleActivityResponse>> get(
            @PathVariable UUID vehicleId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Number of entries to return (1-100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_LIMIT) int limit) {
        return ApiResponse.success(service.recent(user, vehicleId, limit));
    }
}
