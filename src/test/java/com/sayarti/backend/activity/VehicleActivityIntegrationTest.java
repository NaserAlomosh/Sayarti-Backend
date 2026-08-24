package com.sayarti.backend.activity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.AbstractIntegrationTest;
import com.sayarti.backend.auth.repository.EmailVerificationOtpRepository;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.expense.repository.ExpenseRepository;
import com.sayarti.backend.fuel.repository.FuelRecordRepository;
import com.sayarti.backend.maintenance.repository.MaintenanceRecordRepository;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleActivityIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FuelRecordRepository fuel;
    @Autowired MaintenanceRecordRepository maintenance;
    @Autowired ExpenseRepository expenses;
    @Autowired ReminderRepository reminders;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;

    @BeforeEach @AfterEach
    void clearDatabase() {
        reminders.deleteAll(); expenses.deleteAll(); maintenance.deleteAll(); fuel.deleteAll();
        vehicles.deleteAll(); tokens.deleteAll(); otps.deleteAll(); users.deleteAll();
    }

    @Test
    void emptyOwnedVehicleReturnsEmptyActivity() throws Exception {
        Session owner = session("activity-empty@example.com");
        mvc.perform(get(url(vehicle(owner))).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsAllTypesWithBusinessDatesOriginalCurrenciesAndNewestFirst() throws Exception {
        Session owner = session("activity-all@example.com"); UUID vehicle = vehicle(owner);
        createFuel(owner, vehicle, "2026-01-04T10:00:00Z", "JOD");
        createMaintenance(owner, vehicle, "2026-01-03T10:00:00Z", "EUR");
        createExpense(owner, vehicle, "2026-01-02T10:00:00Z", "USD", "Parking");
        UUID completed = createReminder(owner, vehicle, "Completed reminder");
        mvc.perform(patch(reminderUrl(vehicle) + "/" + completed + "/complete")
                .header("Authorization", bearer(owner))).andExpect(status().isOk());
        createReminder(owner, vehicle, "Future active reminder");

        mvc.perform(get(url(vehicle)).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].type").value("REMINDER"))
                .andExpect(jsonPath("$.data[0].title").value("Completed reminder"))
                .andExpect(jsonPath("$.data[0].amount").doesNotExist())
                .andExpect(jsonPath("$.data[0].currencyCode").doesNotExist())
                .andExpect(jsonPath("$.data[1].type").value("FUEL"))
                .andExpect(jsonPath("$.data[1].occurredAt").value("2026-01-04T10:00:00Z"))
                .andExpect(jsonPath("$.data[1].amount").value(20.0))
                .andExpect(jsonPath("$.data[1].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data[2].type").value("MAINTENANCE"))
                .andExpect(jsonPath("$.data[2].occurredAt").value("2026-01-03T10:00:00Z"))
                .andExpect(jsonPath("$.data[2].amount").value(30.0))
                .andExpect(jsonPath("$.data[2].currencyCode").value("EUR"))
                .andExpect(jsonPath("$.data[3].type").value("EXPENSE"))
                .andExpect(jsonPath("$.data[3].occurredAt").value("2026-01-02T10:00:00Z"))
                .andExpect(jsonPath("$.data[3].amount").value(40.0))
                .andExpect(jsonPath("$.data[3].currencyCode").value("USD"));
    }

    @Test
    void excludesSoftDeletedRecordsAndActivityFromAnotherVehicle() throws Exception {
        Session owner = session("activity-delete@example.com");
        UUID vehicle = vehicle(owner); UUID otherVehicle = vehicle(owner);
        UUID deletedExpense = id(createExpense(owner, vehicle,
                "2026-02-01T10:00:00Z", "USD", "Deleted"));
        mvc.perform(delete("/api/v1/vehicles/" + vehicle + "/expenses/" + deletedExpense)
                .header("Authorization", bearer(owner))).andExpect(status().isOk());
        createExpense(owner, otherVehicle, "2026-03-01T10:00:00Z", "EUR", "Other vehicle");
        createExpense(owner, vehicle, "2026-01-01T10:00:00Z", "JOD", "Visible");

        mvc.perform(get(url(vehicle)).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Visible"));
    }

    @Test
    void hidesCrossUserVehicleAndRequiresAuthentication() throws Exception {
        Session owner = session("activity-owner@example.com");
        Session other = session("activity-other@example.com"); UUID vehicle = vehicle(owner);
        createExpense(owner, vehicle, "2026-01-01T10:00:00Z", "JOD", "Private");
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(get(url(vehicle))).andExpect(status().isUnauthorized());
    }

    @Test
    void defaultAndValidatedCustomLimitsAreRespected() throws Exception {
        Session owner = session("activity-limit@example.com"); UUID vehicle = vehicle(owner);
        for (int day = 1; day <= 21; day++) {
            createExpense(owner, vehicle, "2026-01-%02dT10:00:00Z".formatted(day),
                    "JOD", "Expense " + day);
        }
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(20))
                .andExpect(jsonPath("$.data[0].title").value("Expense 21"));
        mvc.perform(get(url(vehicle) + "?limit=3").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));
        mvc.perform(get(url(vehicle) + "?limit=0").header("Authorization", bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private String createFuel(Session s, UUID v, String date, String currency) throws Exception {
        return authenticatedPost(s, "/api/v1/vehicles/" + v + "/fuel-records", """
                {"odometerKm":1100,"quantityLiters":10,"pricePerLiter":2,
                 "currencyCode":"%s","filledAt":"%s","fullTank":true}
                """.formatted(currency, date));
    }
    private String createMaintenance(Session s, UUID v, String date, String currency)
            throws Exception { return authenticatedPost(s, "/api/v1/vehicles/" + v
            + "/maintenance-records", """
            {"category":"INSPECTION","title":"Inspection","serviceDate":"%s",
             "mileageKm":1100,"cost":30,"currencyCode":"%s"}
            """.formatted(date, currency)); }
    private String createExpense(Session s, UUID v, String date, String currency, String title)
            throws Exception { return authenticatedPost(s,
            "/api/v1/vehicles/" + v + "/expenses", """
            {"category":"PARKING","title":"%s","expenseDate":"%s",
             "amount":40,"currencyCode":"%s"}
            """.formatted(title, date, currency)); }
    private UUID createReminder(Session s, UUID v, String title) throws Exception {
        return id(authenticatedPost(s, reminderUrl(v), """
                {"category":"CUSTOM","title":"%s","triggerType":"DATE",
                 "targetDate":"2030-01-01T00:00:00Z"}
                """.formatted(title)));
    }
    private String authenticatedPost(Session s, String path, String body) throws Exception {
        return mvc.perform(post(path).header("Authorization", bearer(s))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }
    private UUID vehicle(Session s) throws Exception { return id(authenticatedPost(s,
            "/api/v1/vehicles", """
            {"brand":"Test","model":"Car","year":2025,"powertrainType":"GASOLINE",
             "fuelType":"GASOLINE_95","fuelTankCapacityLiters":50,"currentMileage":1000}
            """)); }
    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Activity","lastName":"Owner","email":"%s",
                 "password":"StrongPass1","countryCode":"JO"}
                """.formatted(email))).andExpect(status().isCreated());
        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElseThrow();
        user.verifyEmail(); users.saveAndFlush(user);
        String response = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"StrongPass1\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Session(json.readTree(response).path("data").path("accessToken").asText());
    }
    private UUID id(String response) throws Exception { return UUID.fromString(
            json.readTree(response).path("data").path("id").asText()); }
    private String url(UUID v) { return "/api/v1/vehicles/" + v + "/activity"; }
    private String reminderUrl(UUID v) { return "/api/v1/vehicles/" + v + "/reminders"; }
    private String bearer(Session s) { return "Bearer " + s.token(); }
    private record Session(String token) { }
}
