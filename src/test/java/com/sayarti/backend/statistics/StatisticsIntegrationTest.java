package com.sayarti.backend.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class StatisticsIntegrationTest extends AbstractIntegrationTest {
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

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        reminders.deleteAll();
        expenses.deleteAll();
        maintenance.deleteAll();
        fuel.deleteAll();
        vehicles.deleteAll();
        tokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    @Test
    void aggregatesMultipleDomainsCurrenciesCompletionAndExcludesEverySoftDelete()
            throws Exception {
        Session session = session("statistics-data@example.com");
        UUID vehicle = vehicle(session, 54321);

        createFuel(session, vehicle, "10.500", "2", "USD");
        createFuel(session, vehicle, "5.250", "3", "JOD");
        UUID deletedFuel = id(createFuel(session, vehicle, "100", "9", "USD"));
        mvc.perform(delete(fuelUrl(vehicle) + "/" + deletedFuel).header("Authorization", bearer(session)))
                .andExpect(status().isOk());

        createMaintenance(session, vehicle, "25.2500", "USD");
        createMaintenance(session, vehicle, "10", "JOD");
        UUID deletedMaintenance = id(createMaintenance(session, vehicle, "999", "USD"));
        mvc.perform(delete(maintenanceUrl(vehicle) + "/" + deletedMaintenance)
                        .header("Authorization", bearer(session))).andExpect(status().isOk());

        createExpense(session, vehicle, "7.5000", "USD");
        createExpense(session, vehicle, "4", "JOD");
        UUID deletedExpense = id(createExpense(session, vehicle, "888", "USD"));
        mvc.perform(delete(expenseUrl(vehicle) + "/" + deletedExpense)
                        .header("Authorization", bearer(session))).andExpect(status().isOk());

        createReminder(session, vehicle, "Active");
        UUID completed = id(createReminder(session, vehicle, "Completed"));
        mvc.perform(patch(reminderUrl(vehicle) + "/" + completed + "/complete")
                        .header("Authorization", bearer(session))).andExpect(status().isOk());
        UUID deletedReminder = id(createReminder(session, vehicle, "Deleted"));
        mvc.perform(delete(reminderUrl(vehicle) + "/" + deletedReminder)
                        .header("Authorization", bearer(session))).andExpect(status().isOk());

        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(vehicle.toString()))
                .andExpect(jsonPath("$.data.currentMileage").value(54321))
                .andExpect(jsonPath("$.data.totalFuelRecords").value(2))
                .andExpect(jsonPath("$.data.totalFuelQuantity").value(15.750))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency.length()").value(2))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[0].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[0].amount").value(15.7500))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[1].currencyCode").value("USD"))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[1].amount").value(21.0000))
                .andExpect(jsonPath("$.data.totalMaintenanceRecords").value(2))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency.length()").value(2))
                .andExpect(jsonPath("$.data.totalExpenseRecords").value(2))
                .andExpect(jsonPath("$.data.totalExpenseAmountByCurrency.length()").value(2))
                .andExpect(jsonPath("$.data.activeReminderCount").value(1))
                .andExpect(jsonPath("$.data.completedReminderCount").value(1));
    }

    @Test
    void emptyVehicleReturnsZerosAndEmptyAggregateStructures() throws Exception {
        Session session = session("statistics-empty@example.com");
        UUID vehicle = vehicle(session, 12345);
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentMileage").value(12345))
                .andExpect(jsonPath("$.data.totalFuelRecords").value(0))
                .andExpect(jsonPath("$.data.totalFuelQuantity").value(0))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency.length()").value(0))
                .andExpect(jsonPath("$.data.totalMaintenanceRecords").value(0))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency.length()").value(0))
                .andExpect(jsonPath("$.data.totalExpenseRecords").value(0))
                .andExpect(jsonPath("$.data.totalExpenseAmountByCurrency.length()").value(0))
                .andExpect(jsonPath("$.data.activeReminderCount").value(0))
                .andExpect(jsonPath("$.data.completedReminderCount").value(0));
    }

    @Test
    void missingCrossUserAndDeletedVehiclesUseIdenticalNotFoundSemantics() throws Exception {
        Session owner = session("statistics-owner@example.com");
        Session other = session("statistics-other@example.com");
        UUID vehicle = vehicle(owner, 100);
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(get(url(UUID.randomUUID())).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(delete("/api/v1/vehicles/" + vehicle).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get(url(UUID.randomUUID()))).andExpect(status().isUnauthorized());
    }

    private String createFuel(Session session, UUID vehicle, String quantity, String price,
            String currency) throws Exception {
        return mvc.perform(post(fuelUrl(vehicle)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"odometerKm":54000,"quantityLiters":%s,"pricePerLiter":%s,
                         "currencyCode":"%s","filledAt":"2026-08-01T00:00:00Z","fullTank":true}
                        """.formatted(quantity, price, currency))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String createMaintenance(Session session, UUID vehicle, String cost, String currency)
            throws Exception {
        return mvc.perform(post(maintenanceUrl(vehicle)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"category":"GENERAL_SERVICE","title":"Service",
                         "serviceDate":"2026-08-01T00:00:00Z","mileageKm":54000,
                         "cost":%s,"currencyCode":"%s"}
                        """.formatted(cost, currency))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String createExpense(Session session, UUID vehicle, String amount, String currency)
            throws Exception {
        return mvc.perform(post(expenseUrl(vehicle)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"category":"OTHER","title":"Expense",
                         "expenseDate":"2026-08-01T00:00:00Z","amount":%s,
                         "currencyCode":"%s"}
                        """.formatted(amount, currency))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String createReminder(Session session, UUID vehicle, String title) throws Exception {
        return mvc.perform(post(reminderUrl(vehicle)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"category":"CUSTOM","title":"%s","triggerType":"MILEAGE",
                         "targetMileage":60000}
                        """.formatted(title))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private UUID vehicle(Session session, long mileage) throws Exception {
        String response = mvc.perform(post("/api/v1/vehicles").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"brand":"Test","model":"Car","year":2025,
                         "powertrainType":"GASOLINE","fuelType":"GASOLINE_95",
                         "fuelTankCapacityLiters":50,"currentMileage":%d}
                        """.formatted(mileage))).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString();
        return id(response);
    }

    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Statistics","lastName":"Owner","email":"%s",
                 "password":"StrongPass1","countryCode":"JO"}
                """.formatted(email))).andExpect(status().isCreated());
        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElseThrow();
        user.verifyEmail();
        users.saveAndFlush(user);
        String response = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPass1\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = json.readTree(response).path("data");
        return new Session(data.path("accessToken").asText());
    }

    private UUID id(String response) throws Exception {
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }
    private String url(UUID vehicle) { return "/api/v1/vehicles/" + vehicle + "/statistics"; }
    private String fuelUrl(UUID vehicle) { return "/api/v1/vehicles/" + vehicle + "/fuel-records"; }
    private String maintenanceUrl(UUID vehicle) { return "/api/v1/vehicles/" + vehicle + "/maintenance-records"; }
    private String expenseUrl(UUID vehicle) { return "/api/v1/vehicles/" + vehicle + "/expenses"; }
    private String reminderUrl(UUID vehicle) { return "/api/v1/vehicles/" + vehicle + "/reminders"; }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(String token) { }
}
