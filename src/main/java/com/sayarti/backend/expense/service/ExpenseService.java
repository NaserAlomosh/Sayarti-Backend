package com.sayarti.backend.expense.service;

import com.sayarti.backend.common.exception.BusinessValidationException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.common.query.ListQuerySupport;
import com.sayarti.backend.common.response.PageResponse;
import com.sayarti.backend.expense.dto.CreateExpenseRequest;
import com.sayarti.backend.expense.dto.ExpenseResponse;
import com.sayarti.backend.expense.dto.UpdateExpenseRequest;
import com.sayarti.backend.expense.entity.Expense;
import com.sayarti.backend.expense.repository.ExpenseRepository;
import com.sayarti.backend.reference.repository.CurrencyRepository;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "expenseDate", "expenseDate", "createdAt", "createdAt", "amount", "amount", "title", "title");
    private final ExpenseRepository expenses;
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final CurrencyRepository currencies;

    public ExpenseService(ExpenseRepository expenses, VehicleRepository vehicles,
            UserRepository users, CurrencyRepository currencies) {
        this.expenses = expenses;
        this.vehicles = vehicles;
        this.users = users;
        this.currencies = currencies;
    }

    @Transactional
    public ExpenseResponse create(AuthenticatedUser user, UUID vehicleId,
            CreateExpenseRequest request) {
        ownedVehicle(user, vehicleId);
        Expense expense = new Expense(vehicleId, request.category(), request.title(),
                request.expenseDate(), request.amount(), resolveCurrency(user,
                request.currencyCode()), request.notes());
        expenses.save(expense);
        return ExpenseResponse.from(expense);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(AuthenticatedUser user, UUID vehicleId, int page, int size,
            String sortBy, String sortDirection, Instant from, Instant to) {
        ownedVehicle(user, vehicleId);
        ListQuerySupport.validateRange(from, to);
        var pageable = ListQuerySupport.pageable(page, size, sortBy, sortDirection,
                "expenseDate", Sort.Direction.DESC, SORT_FIELDS, "createdAt");
        var result = expenses.findAll((root, query, cb) -> {
            var predicate = cb.and(cb.equal(root.get("vehicleId"), vehicleId),
                    cb.isNull(root.get("deletedAt")));
            if (from != null) predicate = cb.and(predicate, cb.greaterThanOrEqualTo(
                    root.<Instant>get("expenseDate"), from));
            if (to != null) predicate = cb.and(predicate, cb.lessThanOrEqualTo(
                    root.<Instant>get("expenseDate"), to));
            return predicate;
        }, pageable).map(ExpenseResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(AuthenticatedUser user, UUID vehicleId, UUID expenseId) {
        ownedVehicle(user, vehicleId);
        return ExpenseResponse.from(activeExpense(vehicleId, expenseId));
    }

    @Transactional
    public ExpenseResponse update(AuthenticatedUser user, UUID vehicleId, UUID expenseId,
            UpdateExpenseRequest request) {
        ownedVehicle(user, vehicleId);
        Expense expense = activeExpense(vehicleId, expenseId);
        String title = value(request.title(), expense.getTitle());
        if (title.isBlank()) {
            throw new BusinessValidationException(ErrorCode.VALIDATION_ERROR,
                    "Expense title cannot be blank");
        }
        String currency = request.currencyCode() == null ? expense.getCurrencyCode()
                : validateCurrency(request.currencyCode());
        expense.update(value(request.category(), expense.getCategory()), title,
                value(request.expenseDate(), expense.getExpenseDate()),
                value(request.amount(), expense.getAmount()), currency,
                value(request.notes(), expense.getNotes()));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(AuthenticatedUser user, UUID vehicleId, UUID expenseId) {
        ownedVehicle(user, vehicleId);
        activeExpense(vehicleId, expenseId).delete();
    }

    private void ownedVehicle(AuthenticatedUser user, UUID vehicleId) {
        vehicles.findByIdAndUserIdAndDeletedAtIsNull(vehicleId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
    }

    private Expense activeExpense(UUID vehicleId, UUID expenseId) {
        return expenses.findByIdAndVehicleIdAndDeletedAtIsNull(expenseId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.EXPENSE_NOT_FOUND, "Expense not found"));
    }

    private String resolveCurrency(AuthenticatedUser authenticated, String requested) {
        if (requested != null) {
            return validateCurrency(requested);
        }
        User user = users.findByIdAndDeletedAtIsNull(authenticated.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND, "User not found"));
        if (user.getDefaultCurrencyCode() == null) {
            throw new BusinessValidationException(ErrorCode.COUNTRY_SETUP_INCOMPLETE,
                    "A default currency must be configured before recording an expense");
        }
        return validateCurrency(user.getDefaultCurrencyCode());
    }

    private String validateCurrency(String candidate) {
        String code = candidate.trim().toUpperCase(Locale.ROOT);
        if (code.length() != 3 || currencies.findByCodeAndActiveTrue(code).isEmpty()) {
            throw new BusinessValidationException(ErrorCode.CURRENCY_NOT_SUPPORTED,
                    "Currency is not supported");
        }
        return code;
    }

    private <T> T value(T proposed, T existing) { return proposed == null ? existing : proposed; }
}
