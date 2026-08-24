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
import com.sayarti.backend.maintenance.repository.MaintenanceRecordRepository;
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
class MaintenanceStatisticsIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MaintenanceRecordRepository maintenance;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;

    @Test
    void calculatesCostsAveragesLatestRecordAndDeterministicCategoryBreakdown() throws Exception {
        Session session = session("maintenance-statistics@example.com");
        UUID vehicle = vehicle(session);
        createMaintenance(session, vehicle, "OIL_CHANGE", "40.0000", "USD", 12000,
                "2026-06-01T10:00:00Z");
        createMaintenance(session, vehicle, "BRAKE_SERVICE", "80.0000", "USD", 15000,
                "2026-08-20T10:00:00Z");
        createMaintenance(session, vehicle, "OIL_CHANGE", "30.0000", "JOD", 14000,
                "2026-07-01T10:00:00Z");

        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(vehicle.toString()))
                .andExpect(jsonPath("$.data.totalMaintenanceRecords").value(3))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency[0].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency[0].amount").value(30.0))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency[1].currencyCode").value("USD"))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency[1].amount").value(120.0))
                .andExpect(jsonPath("$.data.averageMaintenanceCostByCurrency[0].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.averageMaintenanceCostByCurrency[0].amount").value(30.0))
                .andExpect(jsonPath("$.data.averageMaintenanceCostByCurrency[1].currencyCode").value("USD"))
                .andExpect(jsonPath("$.data.averageMaintenanceCostByCurrency[1].amount").value(60.0))
                .andExpect(jsonPath("$.data.latestMaintenanceDate").value("2026-08-20T10:00:00Z"))
                .andExpect(jsonPath("$.data.latestMaintenanceMileage").value(15000.0))
                .andExpect(jsonPath("$.data.maintenanceByCategory[0].category").value("OIL_CHANGE"))
                .andExpect(jsonPath("$.data.maintenanceByCategory[0].totalMaintenanceRecords").value(2))
                .andExpect(jsonPath("$.data.maintenanceByCategory[0].totalMaintenanceCostByCurrency.length()").value(2))
                .andExpect(jsonPath("$.data.maintenanceByCategory[1].category").value("BRAKE_SERVICE"))
                .andExpect(jsonPath("$.data.maintenanceByCategory[1].totalMaintenanceRecords").value(1));
    }

    @Test
    void excludesSoftDeletedRecordsFromEveryStatisticAndReturnsSafeEmptyResponse() throws Exception {
        Session session = session("maintenance-statistics-edge@example.com");
        UUID vehicle = vehicle(session);
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMaintenanceRecords").value(0))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.averageMaintenanceCostByCurrency").isEmpty())
                .andExpect(jsonPath("$.data.latestMaintenanceDate").doesNotExist())
                .andExpect(jsonPath("$.data.latestMaintenanceMileage").doesNotExist())
                .andExpect(jsonPath("$.data.maintenanceByCategory").isEmpty());

        createMaintenance(session, vehicle, "INSPECTION", "25", "JOD", 11000,
                "2026-01-01T00:00:00Z");
        UUID deleted = id(createMaintenance(session, vehicle, "ENGINE", "999", "USD", 19000,
                "2026-12-01T00:00:00Z"));
        mvc.perform(delete(maintenanceUrl(vehicle) + "/" + deleted)
                .header("Authorization", bearer(session))).andExpect(status().isOk());

        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMaintenanceRecords").value(1))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency.length()").value(1))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency[0].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.totalMaintenanceCostByCurrency[0].amount").value(25.0))
                .andExpect(jsonPath("$.data.averageMaintenanceCostByCurrency[0].amount").value(25.0))
                .andExpect(jsonPath("$.data.latestMaintenanceDate").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.latestMaintenanceMileage").value(11000.0))
                .andExpect(jsonPath("$.data.maintenanceByCategory.length()").value(1))
                .andExpect(jsonPath("$.data.maintenanceByCategory[0].category").value("INSPECTION"));
    }

    @Test
    void hidesMissingDeletedAndOtherUsersVehiclesAndRequiresAuthentication() throws Exception {
        Session owner = session("maintenance-statistics-owner@example.com");
        Session other = session("maintenance-statistics-other@example.com");
        UUID owned = vehicle(owner);
        mvc.perform(get(url(UUID.randomUUID())).header("Authorization", bearer(owner)))
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

    private String createMaintenance(Session session, UUID vehicle, String category, String cost,
            String currency, long mileage, String date) throws Exception {
        return mvc.perform(post(maintenanceUrl(vehicle)).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"category":"%s","title":"Scheduled service","serviceDate":"%s",
                         "mileageKm":%d,"cost":%s,"currencyCode":"%s",
                         "serviceProvider":"Trusted Workshop"}
                        """.formatted(category, date, mileage, cost, currency)))
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
                {"firstName":"Maintenance","lastName":"Owner","email":"%s",
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
    private String url(UUID id) { return "/api/v1/vehicles/" + id + "/statistics/maintenance"; }
    private String maintenanceUrl(UUID id) { return "/api/v1/vehicles/" + id + "/maintenance-records"; }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(String token) { }
}
