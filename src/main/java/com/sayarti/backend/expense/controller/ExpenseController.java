package com.sayarti.backend.expense.controller;

import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.expense.dto.CreateExpenseRequest;
import com.sayarti.backend.expense.dto.DeleteExpenseResponse;
import com.sayarti.backend.expense.dto.ExpenseResponse;
import com.sayarti.backend.expense.dto.UpdateExpenseRequest;
import com.sayarti.backend.expense.service.ExpenseService;
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
@RequestMapping("/api/v1/vehicles/{vehicleId}/expenses")
@Tag(name = "Expenses", description = "V1 financial expense records for active vehicles owned "
        + "by the authenticated user. Access is resource-hidden with not-found responses.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: malformed JSON/date/enum or invalid fields"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing or invalid bearer token"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "VEHICLE_NOT_FOUND or EXPENSE_NOT_FOUND; inaccessible resources hidden"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422",
            description = "Unsupported currency or missing default-currency setup")
})
public class ExpenseController {
    private final ExpenseService service;

    public ExpenseController(ExpenseService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an expense", description = "Requires a valid category, title, "
            + "UTC-aware expense date, and positive DECIMAL(19,4) amount. Currency is normalized "
            + "and validated; omission uses the authenticated user's default currency.")
    public ApiResponse<ExpenseResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Owned active vehicle ID", required = true)
            @PathVariable UUID vehicleId, @Valid @RequestBody CreateExpenseRequest request) {
        return ApiResponse.success(service.create(user, vehicleId, request), "Expense created");
    }

    @GetMapping
    @Operation(summary = "Get expense history", description = "Returns non-deleted financial "
            + "records ordered by expense date descending with deterministic tie-breakers.")
    public ApiResponse<List<ExpenseResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId) {
        return ApiResponse.success(service.list(user, vehicleId));
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Get an expense", description = "Missing, deleted, inaccessible, or "
            + "vehicle-mismatched expenses return EXPENSE_NOT_FOUND.")
    public ApiResponse<ExpenseResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @PathVariable UUID expenseId) {
        return ApiResponse.success(service.get(user, vehicleId, expenseId));
    }

    @PatchMapping("/{expenseId}")
    @Operation(summary = "Partially update an expense", description = "Only supplied values are "
            + "changed; omitted values are preserved. IDs and audit fields are immutable. The "
            + "same amount, string, category, date, and currency rules apply as on creation.")
    public ApiResponse<ExpenseResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateExpenseRequest request) {
        return ApiResponse.success(service.update(user, vehicleId, expenseId, request),
                "Expense updated");
    }

    @DeleteMapping("/{expenseId}")
    @Operation(summary = "Soft-delete an expense", description = "Sets deletedAt and excludes "
            + "the record from history. Later detail, update, and delete calls return "
            + "EXPENSE_NOT_FOUND.")
    public ApiResponse<DeleteExpenseResponse> delete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID vehicleId, @PathVariable UUID expenseId) {
        service.delete(user, vehicleId, expenseId);
        return ApiResponse.success(new DeleteExpenseResponse(true), "Expense deleted");
    }
}
