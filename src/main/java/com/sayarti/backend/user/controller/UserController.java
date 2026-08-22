package com.sayarti.backend.user.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.user.dto.DeleteUserResponse;
import com.sayarti.backend.user.dto.SelectCountryRequest;
import com.sayarti.backend.user.dto.ChangeDefaultCurrencyRequest;
import com.sayarti.backend.user.dto.UpdateUserRequest;
import com.sayarti.backend.user.dto.UserResponse;
import com.sayarti.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Authenticated user profile")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Current user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Authentication required")
    })
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.current(user));
    }

    @PatchMapping("/me")
    @Operation(
            summary = "Update the current authenticated user's profile",
            description = "Only firstName and lastName are editable. Identity and security "
                    + "fields cannot be changed through this endpoint.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Profile updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid profile update"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Authentication required")
    })
    public ApiResponse<UserResponse> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(service.update(user, request), "Profile updated");
    }

    @PatchMapping("/me/country")
    @Operation(summary = "Select the current user's country")
    public ApiResponse<UserResponse> selectCountry(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SelectCountryRequest request) {
        return ApiResponse.success(service.selectCountry(user, request), "Country selected");
    }

    @PatchMapping("/me/default-currency")
    @Operation(summary = "Change the current user's preferred currency")
    public ApiResponse<UserResponse> changeDefaultCurrency(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangeDefaultCurrencyRequest request) {
        return ApiResponse.success(service.changeDefaultCurrency(user, request),
                "Default currency updated");
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "Delete the current authenticated user's account",
            description = "Soft-deletes the account and immediately revokes all of its active "
                    + "refresh tokens. Existing access tokens are rejected because deleted "
                    + "accounts are excluded during authentication.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Account deleted and sessions revoked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Authentication required")
    })
    public ApiResponse<DeleteUserResponse> delete(
            @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(user);
        return ApiResponse.success(new DeleteUserResponse(true), "Account deleted");
    }
}
