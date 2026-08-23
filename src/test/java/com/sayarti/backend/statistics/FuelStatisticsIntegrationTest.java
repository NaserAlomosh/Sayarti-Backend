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
import com.sayarti.backend.fuel.repository.FuelRecordRepository;
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
class FuelStatisticsIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FuelRecordRepository fuel;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        fuel.deleteAll();
        vehicles.deleteAll();
        tokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    @Test
    void calculatesAggregatesAndKeepsCurrenciesSeparate() throws Exception {
        Session session = session("fuel-statistics@example.com");
        UUID vehicle = vehicle(session);
        createFuel(session, vehicle, 1000, "30", "2", "USD", "2026-08-01");
        createFuel(session, vehicle, 1300, "20", "3", "JOD", "2026-08-05");
        createFuel(session, vehicle, 1800, "50", "2", "USD", "2026-08-10");

        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(vehicle.toString()))
                .andExpect(jsonPath("$.data.totalFuelRecords").value(3))
                .andExpect(jsonPath("$.data.totalFuelQuantity").value(100.0))
                .andExpect(jsonPath("$.data.totalDistanceKm").value(800.0))
                .andExpect(jsonPath("$.data.averageFuelEfficiencyKmPerLiter").value(11.4286))
                .andExpect(jsonPath("$.data.averageFuelConsumptionLitersPer100Km").value(8.75))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[0].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[0].amount").value(60.0))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[1].currencyCode").value("USD"))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[1].amount").value(160.0))
                .andExpect(jsonPath("$.data.averageFuelCostPerKmByCurrency[0].amountPerKm")
                        .value(0.075))
                .andExpect(jsonPath("$.data.averageFuelCostPerKmByCurrency[1].amountPerKm")
                        .value(0.2));
    }

    @Test
    void excludesSoftDeletedFuelAndReturnsSafeEmptyAndInsufficientResults() throws Exception {
        Session session = session("fuel-statistics-edge@example.com");
        UUID empty = vehicle(session);
        mvc.perform(get(url(empty)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFuelRecords").value(0))
                .andExpect(jsonPath("$.data.totalFuelQuantity").value(0.0))
                .andExpect(jsonPath("$.data.totalDistanceKm").value(0.0))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.averageFuelCostPerKmByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.averageFuelEfficiencyKmPerLiter").doesNotExist())
                .andExpect(jsonPath("$.data.averageFuelConsumptionLitersPer100Km").doesNotExist());

        UUID active = id(createFuel(session, empty, 1000, "10", "2", "USD", "2026-08-01"));
        UUID deleted = id(createFuel(session, empty, 1100, "99", "9", "USD", "2026-08-02"));
        mvc.perform(delete(fuelUrl(empty) + "/" + deleted)
                .header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(get(url(empty)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFuelRecords").value(1))
                .andExpect(jsonPath("$.data.totalFuelQuantity").value(10.0))
                .andExpect(jsonPath("$.data.totalFuelCostByCurrency[0].amount").value(20.0))
                .andExpect(jsonPath("$.data.totalDistanceKm").value(0.0))
                .andExpect(jsonPath("$.data.averageFuelEfficiencyKmPerLiter").doesNotExist())
                .andExpect(jsonPath("$.data.averageFuelCostPerKmByCurrency[0].amountPerKm")
                        .doesNotExist());
        // Keep the active id used so the test also proves deletion targeted only one record.
        mvc.perform(get(fuelUrl(empty) + "/" + active).header("Authorization", bearer(session)))
                .andExpect(status().isOk());
    }

    @Test
    void hidesMissingDeletedAndOtherUsersVehiclesAndRequiresAuthentication() throws Exception {
        Session owner = session("fuel-statistics-owner@example.com");
        Session other = session("fuel-statistics-other@example.com");
        UUID owned = vehicle(owner);
        UUID missing = UUID.randomUUID();

        mvc.perform(get(url(missing)).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(get(url(owned)).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(delete("/api/v1/vehicles/" + owned).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mvc.perform(get(url(owned)).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(get(url(UUID.randomUUID()))).andExpect(status().isUnauthorized());
    }

    private String createFuel(Session session, UUID vehicle, int odometer, String quantity,
            String price, String currency, String date) throws Exception {
        return mvc.perform(post(fuelUrl(vehicle)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"odometerKm":%d,"quantityLiters":%s,"pricePerLiter":%s,
                         "currencyCode":"%s","filledAt":"%sT10:00:00Z","fullTank":true}
                        """.formatted(odometer, quantity, price, currency, date)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private UUID vehicle(Session session) throws Exception {
        String response = mvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearer(session)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"brand":"Test","model":"Car","year":2025,
                         "powertrainType":"GASOLINE","fuelType":"GASOLINE_95",
                         "fuelTankCapacityLiters":50,"currentMileage":1000}
                        """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return id(response);
    }

    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Fuel","lastName":"Owner","email":"%s",
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
    private String url(UUID id) { return "/api/v1/vehicles/" + id + "/statistics/fuel"; }
    private String fuelUrl(UUID id) { return "/api/v1/vehicles/" + id + "/fuel-records"; }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(String token) { }
}
