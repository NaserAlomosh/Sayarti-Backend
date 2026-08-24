package com.sayarti.backend.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class TrueVehicleCostIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FuelRecordRepository fuel;
    @Autowired MaintenanceRecordRepository maintenance;
    @Autowired ExpenseRepository expenses;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        fuel.deleteAll();
        maintenance.deleteAll();
        expenses.deleteAll();
        vehicles.deleteAll();
        tokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    @Test
    void aggregatesDomainsCurrenciesInclusiveMonthsAndEligibleDistance() throws Exception {
        Session session = session("true-cost@example.com");
        UUID vehicle = vehicle(session);
        createFuel(session, vehicle, 1000, "10", "2", "USD", "2026-01-10");
        createFuel(session, vehicle, 1500, "20", "2", "USD", "2026-03-10");
        createMaintenance(session, vehicle, "30", "JOD", "2026-02-10");
        createExpense(session, vehicle, "60", "USD", "2026-03-20");
        createExpense(session, vehicle, "9", "EUR", "2026-02-20");

        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentMileage").value(1000.0))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[0].amount").value(60.0))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency[0].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency[0].amount").value(30.0))
                .andExpect(jsonPath("$.data.totalExpenseAmountByCurrency.length()").value(2))
                .andExpect(jsonPath("$.data.totalVehicleCostByCurrency[0].currencyCode").value("EUR"))
                .andExpect(jsonPath("$.data.totalVehicleCostByCurrency[0].amount").value(9.0))
                .andExpect(jsonPath("$.data.totalVehicleCostByCurrency[1].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.totalVehicleCostByCurrency[2].currencyCode").value("USD"))
                .andExpect(jsonPath("$.data.totalVehicleCostByCurrency[2].amount").value(120.0))
                .andExpect(jsonPath("$.data.averageMonthlyCostByCurrency[0].amount").value(3.0))
                .andExpect(jsonPath("$.data.averageMonthlyCostByCurrency[1].amount").value(10.0))
                .andExpect(jsonPath("$.data.averageMonthlyCostByCurrency[2].amount").value(40.0))
                .andExpect(jsonPath("$.data.costPerKilometerByCurrency[0].amountPerKm").value(0.018))
                .andExpect(jsonPath("$.data.costPerKilometerByCurrency[1].amountPerKm").value(0.06))
                .andExpect(jsonPath("$.data.costPerKilometerByCurrency[2].amountPerKm").value(0.24));
    }

    @Test
    void sameMonthUsesOneMonthAndInsufficientDistanceReturnsNullRates() throws Exception {
        Session session = session("true-cost-month@example.com");
        UUID vehicle = vehicle(session);
        createExpense(session, vehicle, "12", "USD", "2026-03-01");
        createMaintenance(session, vehicle, "8", "USD", "2026-03-31");
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageMonthlyCostByCurrency[0].amount").value(20.0))
                .andExpect(jsonPath("$.data.costPerKilometerByCurrency[0].amountPerKm").doesNotExist());
    }

    @Test
    void excludesEverySoftDeletedDomainAndReturnsEmptyCollections() throws Exception {
        Session session = session("true-cost-deleted@example.com");
        UUID vehicle = vehicle(session);
        UUID fuelId = id(createFuel(session, vehicle, 1000, "10", "2", "USD", "2026-01-01"));
        UUID maintenanceId = id(createMaintenance(session, vehicle, "30", "JOD", "2026-01-01"));
        UUID expenseId = id(createExpense(session, vehicle, "40", "EUR", "2026-01-01"));
        mvc.perform(delete("/api/v1/vehicles/" + vehicle + "/fuel-records/" + fuelId)
                .header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/vehicles/" + vehicle + "/maintenance-records/" + maintenanceId)
                .header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/vehicles/" + vehicle + "/expenses/" + expenseId)
                .header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentMileage").value(1000.0))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.totalExpenseAmountByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.totalVehicleCostByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.averageMonthlyCostByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.costPerKilometerByCurrency").isEmpty());
    }

    @Test
    void hidesMissingDeletedAndCrossUserVehiclesAndRequiresAuthentication() throws Exception {
        Session owner = session("true-cost-owner@example.com");
        Session other = session("true-cost-other@example.com");
        UUID vehicle = vehicle(owner);
        mvc.perform(get(url(UUID.randomUUID())).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(delete("/api/v1/vehicles/" + vehicle).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(get(url(UUID.randomUUID()))).andExpect(status().isUnauthorized());
    }

    private String createFuel(Session s, UUID v, int km, String quantity, String price,
            String currency, String date) throws Exception {
        return authenticatedPost(s, "/api/v1/vehicles/" + v + "/fuel-records", """
                {"odometerKm":%d,"quantityLiters":%s,"pricePerLiter":%s,
                 "currencyCode":"%s","filledAt":"%sT10:00:00Z","fullTank":true}
                """.formatted(km, quantity, price, currency, date));
    }

    private String createMaintenance(Session s, UUID v, String cost, String currency, String date)
            throws Exception {
        return authenticatedPost(s, "/api/v1/vehicles/" + v + "/maintenance-records", """
                {"category":"INSPECTION","title":"Service","serviceDate":"%sT10:00:00Z",
                 "mileageKm":1000,"cost":%s,"currencyCode":"%s"}
                """.formatted(date, cost, currency));
    }

    private String createExpense(Session s, UUID v, String amount, String currency, String date)
            throws Exception {
        return authenticatedPost(s, "/api/v1/vehicles/" + v + "/expenses", """
                {"category":"TOLL","title":"Expense","expenseDate":"%sT10:00:00Z",
                 "amount":%s,"currencyCode":"%s"}
                """.formatted(date, amount, currency));
    }

    private String authenticatedPost(Session session, String path, String body) throws Exception {
        return mvc.perform(post(path).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private UUID vehicle(Session session) throws Exception {
        return id(authenticatedPost(session, "/api/v1/vehicles", """
                {"brand":"Test","model":"Car","year":2025,"powertrainType":"GASOLINE",
                 "fuelType":"GASOLINE_95","fuelTankCapacityLiters":50,"currentMileage":1000}
                """));
    }

    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Cost","lastName":"Owner","email":"%s",
                 "password":"StrongPass1","countryCode":"JO"}
                """.formatted(email))).andExpect(status().isCreated());
        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElseThrow();
        user.verifyEmail();
        users.saveAndFlush(user);
        String response = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPass1\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Session(json.readTree(response).path("data").path("accessToken").asText());
    }

    private UUID id(String response) throws Exception {
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }

    private String url(UUID id) { return "/api/v1/vehicles/" + id + "/statistics/total-cost"; }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(String token) { }
}
