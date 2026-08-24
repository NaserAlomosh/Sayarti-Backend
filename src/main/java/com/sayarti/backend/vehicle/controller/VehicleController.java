package com.sayarti.backend.vehicle.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.common.response.PageResponse;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.vehicle.dto.CreateVehicleRequest;
import com.sayarti.backend.vehicle.dto.DeleteVehicleResponse;
import com.sayarti.backend.vehicle.dto.UpdateMileageRequest;
import com.sayarti.backend.vehicle.dto.UpdateVehicleRequest;
import com.sayarti.backend.vehicle.dto.VehicleResponse;
import com.sayarti.backend.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Authenticated vehicle management. Ownership is derived "
        + "only from the bearer token; inaccessible and soft-deleted vehicles return "
        + "VEHICLE_NOT_FOUND.")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {
    private final VehicleService service;
    public VehicleController(VehicleService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a vehicle", description = "Powertrain configuration is validated. "
            + "Electric vehicles require battery capacity and range and forbid liquid-fuel fields; "
            + "combustion and hybrid vehicles require a compatible fuel type.")
    public ApiResponse<VehicleResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateVehicleRequest request) {
        return ApiResponse.success(service.create(user, request), "Vehicle created");
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's active vehicles", description = "Returns "
            + "a paginated active-only list. Defaults to createdAt descending. Supported sortBy "
            + "values: createdAt, brand, model, year, currentMileage; sortDirection is asc or desc. "
            + "Equal values use id as a deterministic tie-breaker. Empty pages are successful.")
    public ApiResponse<PageResponse<VehicleResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        return ApiResponse.success(service.list(user, page, size, sortBy, sortDirection));
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get an owned vehicle", description = "Other users' vehicles, deleted "
            + "vehicles, and unknown identifiers all return VEHICLE_NOT_FOUND.")
    public ApiResponse<VehicleResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId) { return ApiResponse.success(service.get(user, vehicleId)); }

    @PatchMapping("/{vehicleId}")
    @Operation(summary = "Update editable vehicle details", description = "Mileage and internal "
            + "ownership/audit fields are not editable here. A powertrain change revalidates its "
            + "complete fuel and battery configuration.")
    public ApiResponse<VehicleResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @Valid @RequestBody UpdateVehicleRequest request) {
        return ApiResponse.success(service.update(user, vehicleId, request), "Vehicle updated");
    }

    @PatchMapping("/{vehicleId}/mileage")
    @Operation(summary = "Update vehicle mileage", description = "Mileage may remain equal or "
            + "increase. A decrease returns INVALID_VEHICLE_MILEAGE.")
    public ApiResponse<VehicleResponse> mileage(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @Valid @RequestBody UpdateMileageRequest request) {
        return ApiResponse.success(service.updateMileage(user, vehicleId, request.mileage()),
                "Vehicle mileage updated");
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Soft-delete an owned vehicle", description = "The row is retained with "
            + "deletedAt and excluded from all normal vehicle reads.")
    public ApiResponse<DeleteVehicleResponse> delete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId) {
        service.delete(user, vehicleId);
        return ApiResponse.success(new DeleteVehicleResponse(true), "Vehicle deleted");
    }
}
