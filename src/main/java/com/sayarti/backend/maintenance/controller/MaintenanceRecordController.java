package com.sayarti.backend.maintenance.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.maintenance.dto.CreateMaintenanceRecordRequest;
import com.sayarti.backend.maintenance.dto.DeleteMaintenanceRecordResponse;
import com.sayarti.backend.maintenance.dto.MaintenanceRecordResponse;
import com.sayarti.backend.maintenance.dto.UpdateMaintenanceRecordRequest;
import com.sayarti.backend.maintenance.service.MaintenanceRecordService;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/maintenance-records")
@Tag(name = "Maintenance", description = "Authenticated vehicle maintenance CRUD. The vehicle "
        + "must be active and owned by the bearer-token user. Inaccessible records use "
        + "resource-hiding not-found responses.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Malformed request or Bean Validation failure"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing or invalid bearer token"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Vehicle or maintenance record is inaccessible, deleted, or missing"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422",
            description = "Unsupported currency or maintenance business validation failure")
})
public class MaintenanceRecordController {
    private final MaintenanceRecordService service;

    public MaintenanceRecordController(MaintenanceRecordService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a maintenance record", description = "Validates category, title, "
            + "service date, non-negative whole-kilometer mileage, cost, and supported currency. "
            + "An omitted currency uses the user's default currency.")
    public ApiResponse<MaintenanceRecordResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Owned active vehicle identifier", required = true)
            @PathVariable UUID vehicleId,
            @Valid @RequestBody CreateMaintenanceRecordRequest request) {
        return ApiResponse.success(service.create(user, vehicleId, request),
                "Maintenance record created");
    }

    @GetMapping
    @Operation(summary = "Get maintenance history", description = "Returns active records newest "
            + "service date first, with creation time and identifier tie-breakers.")
    public ApiResponse<List<MaintenanceRecordResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Owned active vehicle identifier", required = true)
            @PathVariable UUID vehicleId) {
        return ApiResponse.success(service.list(user, vehicleId));
    }

    @GetMapping("/{maintenanceRecordId}")
    @Operation(summary = "Get a maintenance record", description = "Missing, deleted, or "
            + "vehicle-mismatched records return MAINTENANCE_NOT_FOUND.")
    public ApiResponse<MaintenanceRecordResponse> get(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID vehicleId,
            @Parameter(description = "Maintenance record identifier", required = true)
            @PathVariable UUID maintenanceRecordId) {
        return ApiResponse.success(service.get(user, vehicleId, maintenanceRecordId));
    }

    @PatchMapping("/{maintenanceRecordId}")
    @Operation(summary = "Partially update a maintenance record", description = "Only supplied "
            + "fields change; omitted fields are retained. Vehicle and audit fields are immutable. "
            + "Validation is identical to creation.")
    public ApiResponse<MaintenanceRecordResponse> update(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID vehicleId,
            @PathVariable UUID maintenanceRecordId,
            @Valid @RequestBody UpdateMaintenanceRecordRequest request) {
        return ApiResponse.success(service.update(user, vehicleId, maintenanceRecordId, request),
                "Maintenance record updated");
    }

    @DeleteMapping("/{maintenanceRecordId}")
    @Operation(summary = "Soft-delete a maintenance record", description = "The retained row is "
            + "excluded from history and cannot subsequently be read, changed, or deleted again.")
    public ApiResponse<DeleteMaintenanceRecordResponse> delete(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID vehicleId,
            @PathVariable UUID maintenanceRecordId) {
        service.delete(user, vehicleId, maintenanceRecordId);
        return ApiResponse.success(new DeleteMaintenanceRecordResponse(true),
                "Maintenance record deleted");
    }
}
