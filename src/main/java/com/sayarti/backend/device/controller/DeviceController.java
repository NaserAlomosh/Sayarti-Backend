package com.sayarti.backend.device.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.device.dto.DeleteDeviceResponse;
import com.sayarti.backend.device.dto.DeviceResponse;
import com.sayarti.backend.device.dto.RegisterDeviceRequest;
import com.sayarti.backend.device.dto.UpdateFcmTokenRequest;
import com.sayarti.backend.device.service.DeviceService;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Authenticated mobile push-registration lifecycle. "
        + "Responses never expose FCM tokens, and inaccessible registrations are hidden.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: malformed JSON/platform or invalid fields"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing or invalid bearer token"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "DEVICE_NOT_FOUND: missing or another user's device is hidden")
})
public class DeviceController {
    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register or refresh a device", description = "Idempotent for the "
            + "authenticated user and stable deviceIdentifier: reuses the row and refreshes its "
            + "ANDROID/IOS platform and FCM token. If Firebase has reassigned the token, its old "
            + "registration is removed so only one active registration can receive a message.")
    public ApiResponse<DeviceResponse> register(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RegisterDeviceRequest request) {
        return ApiResponse.success(service.register(user, request), "Device registered");
    }

    @PatchMapping("/{deviceId}/fcm-token")
    @Operation(summary = "Rotate a device FCM token", description = "Updates an owned device "
            + "without creating a row. A token is safely reassigned from any stale registration "
            + "to prevent duplicate notifications. FCM tokens are never returned.")
    public ApiResponse<DeviceResponse> updateToken(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID deviceId, @Valid @RequestBody UpdateFcmTokenRequest request) {
        return ApiResponse.success(service.updateToken(user, deviceId, request),
                "FCM token updated");
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Remove a device registration", description = "Permanently removes an "
            + "owned registration. Missing and cross-user identifiers both return "
            + "DEVICE_NOT_FOUND.")
    public ApiResponse<DeleteDeviceResponse> delete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID deviceId) {
        service.delete(user, deviceId);
        return ApiResponse.success(new DeleteDeviceResponse(true), "Device deleted");
    }
}
