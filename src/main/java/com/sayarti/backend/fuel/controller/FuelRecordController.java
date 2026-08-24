package com.sayarti.backend.fuel.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.common.response.PageResponse;
import com.sayarti.backend.fuel.dto.CreateFuelRecordRequest;
import com.sayarti.backend.fuel.dto.DeleteFuelRecordResponse;
import com.sayarti.backend.fuel.dto.FuelRecordResponse;
import com.sayarti.backend.fuel.dto.FuelSummaryResponse;
import com.sayarti.backend.fuel.dto.UpdateFuelRecordRequest;
import com.sayarti.backend.fuel.service.FuelRecordService;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.YearMonth;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/fuel-records")
@Tag(name = "Fuel Records", description = "Fuel tracking for owned GASOLINE, DIESEL, HYBRID, "
        + "and PLUG_IN_HYBRID vehicles. Other users' and soft-deleted resources use not-found "
        + "semantics. Records retain their original currency and are soft deleted.")
@SecurityRequirement(name = "bearerAuth")
public class FuelRecordController {
    private final FuelRecordService service;
    public FuelRecordController(FuelRecordService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a fuel record", description = "The backend calculates totalCost "
            + "as quantityLiters multiplied by pricePerLiter. An omitted currencyCode falls back "
            + "to the authenticated user's default. Odometer must equal or exceed current vehicle "
            + "mileage; a greater value updates vehicle mileage in the same transaction. Electric "
            + "vehicles are not supported.")
    public ApiResponse<FuelRecordResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @Valid @RequestBody CreateFuelRecordRequest request) {
        return ApiResponse.success(service.create(user, vehicleId, request), "Fuel record created");
    }

    @GetMapping
    @Operation(summary = "List active fuel records", description = "Returns an active-only paginated "
            + "history. Defaults to filledAt descending. Supported sortBy values: filledAt, createdAt, odometerKm, totalCost; "
            + "sortDirection is asc or desc and id is the deterministic tie-breaker. Optional from "
            + "and to are inclusive ISO-8601 UTC-aware instants applied to filledAt; from must not "
            + "exceed to. Filters compose with ownership, soft deletion, sorting, and pagination. "
            + "Empty pages are successful; inaccessible vehicles return VEHICLE_NOT_FOUND.")
    public ApiResponse<PageResponse<FuelRecordResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID vehicleId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ApiResponse.success(service.list(user, vehicleId, page, size, sortBy,
                sortDirection, from, to));
    }

    @GetMapping("/{fuelRecordId}")
    @Operation(summary = "Get an active fuel record", description = "Ownership is checked through "
            + "the vehicle; inaccessible, deleted, or mismatched records are not exposed.")
    public ApiResponse<FuelRecordResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @PathVariable UUID fuelRecordId) {
        return ApiResponse.success(service.get(user, vehicleId, fuelRecordId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Calculate fuel summary", description = "Requires authentication and "
            + "vehicle ownership. The optional month uses YYYY-MM and UTC calendar boundaries; "
            + "it defaults to the current UTC month. Active records are ordered by filledAt, "
            + "odometer, creation time, and id, independent of insertion order. Positive adjacent "
            + "odometer differences contribute distance and the later refill's liters. Aggregate "
            + "km/L is total eligible distance / total eligible liters; L/100km is the inverse "
            + "formula. Consumption and cost/km are null if no positive-distance pair exists. "
            + "Electric-only vehicles return liquidFuelCalculationsSupported=false and null "
            + "consumption values. Costs are grouped by retained currency without conversion.")
    public ApiResponse<FuelSummaryResponse> summary(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID vehicleId,
            @RequestParam(required = false) YearMonth month) {
        YearMonth requested = month == null ? YearMonth.now(java.time.Clock.systemUTC()) : month;
        return ApiResponse.success(service.summary(user, vehicleId, requested));
    }

    @PatchMapping("/{fuelRecordId}")
    @Operation(summary = "Update editable fuel fields", description = "Odometer, vehicle, and audit "
            + "fields are immutable. Changing quantity or price recalculates totalCost; no currency "
            + "conversion is performed.")
    public ApiResponse<FuelRecordResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @PathVariable UUID fuelRecordId,
            @Valid @RequestBody UpdateFuelRecordRequest request) {
        return ApiResponse.success(service.update(user, vehicleId, fuelRecordId, request),
                "Fuel record updated");
    }

    @DeleteMapping("/{fuelRecordId}")
    @Operation(summary = "Soft-delete a fuel record", description = "Sets deletedAt without "
            + "removing the row and never decreases vehicle mileage.")
    public ApiResponse<DeleteFuelRecordResponse> delete(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID vehicleId,
            @PathVariable UUID fuelRecordId) {
        service.delete(user, vehicleId, fuelRecordId);
        return ApiResponse.success(new DeleteFuelRecordResponse(true), "Fuel record deleted");
    }
}
