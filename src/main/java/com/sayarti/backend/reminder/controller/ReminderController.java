package com.sayarti.backend.reminder.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.reminder.dto.CreateReminderRequest;
import com.sayarti.backend.reminder.dto.DeleteReminderResponse;
import com.sayarti.backend.reminder.dto.ReminderResponse;
import com.sayarti.backend.reminder.dto.UpdateReminderRequest;
import com.sayarti.backend.reminder.service.ReminderService;
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
@RequestMapping("/api/v1/vehicles/{vehicleId}/reminders")
@Tag(name = "Reminders", description = "V1 date- and mileage-based reminders for vehicles "
        + "owned by the authenticated user; inaccessible resources are hidden as not found.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: malformed JSON/enum or invalid fields"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing or invalid bearer token"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "VEHICLE_NOT_FOUND or REMINDER_NOT_FOUND; inaccessible resources hidden"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422",
            description = "INVALID_REMINDER: invalid DATE/MILEAGE target combination")
})
public class ReminderController {
    private final ReminderService service;

    public ReminderController(ReminderService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a reminder", description = "DATE requires targetDate and no "
            + "targetMileage; MILEAGE requires a non-negative whole-kilometer targetMileage and "
            + "no targetDate.")
    public ApiResponse<ReminderResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Owned active vehicle ID", required = true)
            @PathVariable UUID vehicleId, @Valid @RequestBody CreateReminderRequest request) {
        return ApiResponse.success(service.create(user, vehicleId, request), "Reminder created");
    }

    @GetMapping
    @Operation(summary = "Get reminder history", description = "Returns active and completed "
            + "reminders, excluding soft-deleted reminders, newest first.")
    public ApiResponse<List<ReminderResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId) {
        return ApiResponse.success(service.list(user, vehicleId));
    }

    @GetMapping("/{reminderId}")
    @Operation(summary = "Get a reminder", description = "Completed reminders remain visible. "
            + "Deleted, missing, inaccessible, or vehicle-mismatched records are not found.")
    public ApiResponse<ReminderResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @PathVariable UUID reminderId) {
        return ApiResponse.success(service.get(user, vehicleId, reminderId));
    }

    @PatchMapping("/{reminderId}")
    @Operation(summary = "Partially update a reminder", description = "Omitted fields are "
            + "preserved. Changing trigger type requires its corresponding target and clears the "
            + "previous trigger target.")
    public ApiResponse<ReminderResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @PathVariable UUID reminderId,
            @Valid @RequestBody UpdateReminderRequest request) {
        return ApiResponse.success(service.update(user, vehicleId, reminderId, request),
                "Reminder updated");
    }

    @PatchMapping("/{reminderId}/complete")
    @Operation(summary = "Complete a reminder", description = "Idempotently marks the reminder "
            + "completed and records a UTC completion timestamp. No notification is sent.")
    public ApiResponse<ReminderResponse> complete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @PathVariable UUID reminderId) {
        return ApiResponse.success(service.complete(user, vehicleId, reminderId),
                "Reminder completed");
    }

    @DeleteMapping("/{reminderId}")
    @Operation(summary = "Soft-delete a reminder", description = "Sets deletedAt and hides the "
            + "record from later list, detail, update, completion, and deletion operations.")
    public ApiResponse<DeleteReminderResponse> delete(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID vehicleId,
            @PathVariable UUID reminderId) {
        service.delete(user, vehicleId, reminderId);
        return ApiResponse.success(new DeleteReminderResponse(true), "Reminder deleted");
    }
}
