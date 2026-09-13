package com.sayarti.backend.dashboard;

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
class DashboardIntegrationTest extends AbstractIntegrationTest {
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

    @Test
    void aggregatesAllDomainsCurrenciesCalculationsAndUpcomingReminder() throws Exception {
        Session session = session("dashboard-all@example.com");
        UUID vehicle = vehicle(session);
        createFuel(session, vehicle, 1000, "10", "2", "USD", "2026-01-10");
        createFuel(session, vehicle, 1500, "20", "2", "USD", "2026-03-10");
        createMaintenance(session, vehicle, "30", "JOD", "2026-02-10");
        createExpense(session, vehicle, "60", "USD", "2026-03-20");
        createExpense(session, vehicle, "9", "EUR", "2026-02-20");
        createReminder(session, vehicle, "DATE", "2028-02-01T00:00:00Z", null, "Later");
        createReminder(session, vehicle, "DATE", "2027-02-01T00:00:00Z", null, "Nearest");
        UUID completed = createReminder(session, vehicle, "MILEAGE", null, 2000L, "Complete");
        mvc.perform(patch(reminderUrl(vehicle) + "/" + completed + "/complete")
                .header("Authorization", bearer(session))).andExpect(status().isOk());

        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(vehicle.toString()))
                .andExpect(jsonPath("$.data.vehicle.brand").value("Test"))
                .andExpect(jsonPath("$.data.currentMileage").value(1500))
                .andExpect(jsonPath("$.data.vehicle.currentMileage").value(1500))
                .andExpect(jsonPath("$.data.fuel.totalRecords").value(2))
                .andExpect(jsonPath("$.data.fuel.totalQuantityLiters").value(30.0))
                .andExpect(jsonPath("$.data.fuel.averageKmPerLiter").value(25.0))
                .andExpect(jsonPath("$.data.fuel.averageLitersPer100Km").value(4.0))
                .andExpect(jsonPath("$.data.maintenance.totalRecords").value(1))
                .andExpect(jsonPath("$.data.maintenance.latestMaintenanceDate").value("2026-02-10T10:00:00Z"))
                .andExpect(jsonPath("$.data.expenses.totalRecords").value(2))
                .andExpect(jsonPath("$.data.expenses.costsByCurrency.length()").value(2))
                .andExpect(jsonPath("$.data.totalVehicleCost.costsByCurrency.length()").value(3))
                .andExpect(jsonPath("$.data.totalVehicleCost.costsByCurrency[2].amount").value(120.0))
                .andExpect(jsonPath("$.data.totalVehicleCost.averageMonthlyCostsByCurrency[2].amount").value(40.0))
                .andExpect(jsonPath("$.data.totalVehicleCost.costsPerKilometerByCurrency[2].amountPerKm").value(0.24))
                .andExpect(jsonPath("$.data.reminders.activeCount").value(2))
                .andExpect(jsonPath("$.data.reminders.completedCount").value(1))
                .andExpect(jsonPath("$.data.reminders.nearestUpcoming.title").value("Nearest"));
    }

    @Test
    void emptyAndInsufficientHistoryAreStableAndMileageReminderIsFallback() throws Exception {
        Session session = session("dashboard-empty@example.com"); UUID vehicle = vehicle(session);
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fuel.totalRecords").value(0))
                .andExpect(jsonPath("$.data.fuel.totalQuantityLiters").value(0.0))
                .andExpect(jsonPath("$.data.fuel.costsByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.maintenance.totalRecords").value(0))
                .andExpect(jsonPath("$.data.expenses.totalRecords").value(0))
                .andExpect(jsonPath("$.data.totalVehicleCost.costsByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.reminders.activeCount").value(0))
                .andExpect(jsonPath("$.data.reminders.nearestUpcoming").doesNotExist());
        createFuel(session, vehicle, 1000, "10", "2", "USD", "2026-01-10");
        createReminder(session, vehicle, "MILEAGE", null, 3000L, "Far");
        createReminder(session, vehicle, "MILEAGE", null, 1500L, "Near mileage");
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fuel.averageKmPerLiter").doesNotExist())
                .andExpect(jsonPath("$.data.fuel.averageLitersPer100Km").doesNotExist())
                .andExpect(jsonPath("$.data.maintenance.totalRecords").value(0))
                .andExpect(jsonPath("$.data.maintenance.latestMaintenanceDate").doesNotExist())
                .andExpect(jsonPath("$.data.maintenance.costsByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.expenses.costsByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.reminders.nearestUpcoming.title").value("Near mileage"));
    }

    @Test
    void excludesSoftDeletedRecordsAndReminder() throws Exception {
        Session s = session("dashboard-deleted-records@example.com"); UUID v = vehicle(s);
        UUID f = id(createFuel(s, v, 1000, "10", "2", "USD", "2026-01-10"));
        UUID m = id(createMaintenance(s, v, "30", "JOD", "2026-02-10"));
        UUID e = id(createExpense(s, v, "9", "EUR", "2026-02-20"));
        UUID r = createReminder(s, v, "DATE", "2027-02-01T00:00:00Z", null, "Deleted");
        remove(s, "/api/v1/vehicles/" + v + "/fuel-records/" + f);
        remove(s, "/api/v1/vehicles/" + v + "/maintenance-records/" + m);
        remove(s, "/api/v1/vehicles/" + v + "/expenses/" + e);
        remove(s, reminderUrl(v) + "/" + r);
        mvc.perform(get(url(v)).header("Authorization", bearer(s))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fuel.totalRecords").value(0))
                .andExpect(jsonPath("$.data.maintenance.totalRecords").value(0))
                .andExpect(jsonPath("$.data.expenses.totalRecords").value(0))
                .andExpect(jsonPath("$.data.totalVehicleCost.costsByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.reminders.activeCount").value(0))
                .andExpect(jsonPath("$.data.reminders.nearestUpcoming").doesNotExist());
    }

    @Test
    void hidesMissingDeletedAndCrossUserVehiclesAndRequiresAuthentication() throws Exception {
        Session owner = session("dashboard-owner@example.com");
        Session other = session("dashboard-other@example.com"); UUID vehicle = vehicle(owner);
        notFound(owner, UUID.randomUUID()); notFound(other, vehicle);
        remove(owner, "/api/v1/vehicles/" + vehicle); notFound(owner, vehicle);
        mvc.perform(get(url(UUID.randomUUID()))).andExpect(status().isUnauthorized());
    }

    private void notFound(Session s, UUID v) throws Exception { mvc.perform(get(url(v))
            .header("Authorization", bearer(s))).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND")); }
    private void remove(Session s, String path) throws Exception { mvc.perform(delete(path)
            .header("Authorization", bearer(s))).andExpect(status().isOk()); }
    private String createFuel(Session s, UUID v, int km, String quantity, String price,
            String currency, String date) throws Exception { return authenticatedPost(s,
            "/api/v1/vehicles/" + v + "/fuel-records", """
            {"odometerKm":%d,"quantityLiters":%s,"pricePerLiter":%s,"currencyCode":"%s",
             "filledAt":"%sT10:00:00Z","fullTank":true}
            """.formatted(km, quantity, price, currency, date)); }
    private String createMaintenance(Session s, UUID v, String cost, String currency, String date)
            throws Exception { return authenticatedPost(s, "/api/v1/vehicles/" + v
            + "/maintenance-records", """
            {"category":"INSPECTION","title":"Service","serviceDate":"%sT10:00:00Z",
             "mileageKm":1000,"cost":%s,"currencyCode":"%s"}
            """.formatted(date, cost, currency)); }
    private String createExpense(Session s, UUID v, String amount, String currency, String date)
            throws Exception { return authenticatedPost(s, "/api/v1/vehicles/" + v + "/expenses", """
            {"category":"TOLL","title":"Expense","expenseDate":"%sT10:00:00Z",
             "amount":%s,"currencyCode":"%s"}
            """.formatted(date, amount, currency)); }
    private UUID createReminder(Session s, UUID v, String type, String date, Long mileage,
            String title) throws Exception { String trigger = type.equals("DATE")
            ? "\"targetDate\":\"" + date + "\"" : "\"targetMileage\":" + mileage;
        return id(authenticatedPost(s, reminderUrl(v), """
            {"category":"CUSTOM","title":"%s","triggerType":"%s",%s}
            """.formatted(title, type, trigger))); }
    private String authenticatedPost(Session s, String path, String body) throws Exception {
        return mvc.perform(post(path).header("Authorization", bearer(s))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(); }
    private UUID vehicle(Session s) throws Exception { return id(authenticatedPost(s,
            "/api/v1/vehicles", """
            {"brand":"Test","model":"Car","year":2025,"powertrainType":"GASOLINE",
             "fuelType":"GASOLINE_95","fuelTankCapacityLiters":50,"currentMileage":1000}
            """)); }
    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
            {"firstName":"Dash","lastName":"Owner","email":"%s",
             "password":"StrongPass1","countryCode":"JO"}
            """.formatted(email))).andExpect(status().isCreated());
        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElseThrow();
        user.verifyEmail(); users.saveAndFlush(user);
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"StrongPass1\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Session(json.readTree(response).path("data").path("accessToken").asText());
    }
    private UUID id(String response) throws Exception { return UUID.fromString(
            json.readTree(response).path("data").path("id").asText()); }
    private String url(UUID v) { return "/api/v1/vehicles/" + v + "/dashboard"; }
    private String reminderUrl(UUID v) { return "/api/v1/vehicles/" + v + "/reminders"; }
    private String bearer(Session s) { return "Bearer " + s.token(); }
    private record Session(String token) { }
}
