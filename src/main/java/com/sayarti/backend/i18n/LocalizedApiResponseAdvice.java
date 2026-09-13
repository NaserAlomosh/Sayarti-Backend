package com.sayarti.backend.i18n;

import com.sayarti.backend.common.response.ApiResponse;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class LocalizedApiResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final Map<String, String> KEYS = Map.ofEntries(
            Map.entry("Vehicle created", "success.vehicle.created"), Map.entry("Vehicle updated", "success.vehicle.updated"),
            Map.entry("Vehicle mileage updated", "success.vehicle.mileage.updated"), Map.entry("Vehicle deleted", "success.vehicle.deleted"),
            Map.entry("Fuel record created", "success.fuel.created"), Map.entry("Fuel record updated", "success.fuel.updated"), Map.entry("Fuel record deleted", "success.fuel.deleted"),
            Map.entry("Maintenance record created", "success.maintenance.created"), Map.entry("Maintenance record updated", "success.maintenance.updated"), Map.entry("Maintenance record deleted", "success.maintenance.deleted"),
            Map.entry("Expense created", "success.expense.created"), Map.entry("Expense updated", "success.expense.updated"), Map.entry("Expense deleted", "success.expense.deleted"),
            Map.entry("Reminder created", "success.reminder.created"), Map.entry("Reminder updated", "success.reminder.updated"), Map.entry("Reminder completed", "success.reminder.completed"), Map.entry("Reminder deleted", "success.reminder.deleted"),
            Map.entry("Device registered", "success.device.registered"), Map.entry("Device updated", "success.device.updated"), Map.entry("Device deleted", "success.device.deleted"),
            Map.entry("Profile updated", "success.profile.updated"), Map.entry("Country selected", "success.country.selected"), Map.entry("Default currency updated", "success.currency.updated"), Map.entry("Account deleted", "success.account.deleted"),
            Map.entry("Email verification required", "success.auth.verification.required"), Map.entry("Email verified", "success.auth.email.verified"),
            Map.entry("If the account is eligible, a verification email has been requested", "success.auth.verification.resent"), Map.entry("Logout successful", "success.auth.logout"));
    private final MessageLocalizer localizer;
    public LocalizedApiResponseAdvice(MessageLocalizer localizer) { this.localizer = localizer; }
    @Override public boolean supports(MethodParameter p, Class<? extends HttpMessageConverter<?>> c) { return true; }
    @Override public Object beforeBodyWrite(Object body, MethodParameter p, MediaType m,
            Class<? extends HttpMessageConverter<?>> c, org.springframework.http.server.ServerHttpRequest req,
            org.springframework.http.server.ServerHttpResponse res) {
        if (body instanceof ApiResponse<?> api && api.message() != null) {
            String key = KEYS.get(api.message());
            if (key != null) return new ApiResponse<>(api.success(), api.data(), localizer.text(key, api.message()));
        }
        return body;
    }
}
