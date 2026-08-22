package com.sayarti.backend.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaintenanceRecordIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MaintenanceRecordRepository maintenanceRecords;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        maintenanceRecords.deleteAll();
        vehicles.deleteAll();
        tokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    @Test
    void createsGetsAndUsesDefaultCurrency() throws Exception {
        Session session = session("maintenance-create@example.com");
        UUID vehicleId = vehicle(session, 10000);
        String response = create(session, vehicleId, body(10500, null, "OIL_CHANGE",
                "2026-08-20T10:00:00Z")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.category").value("OIL_CHANGE"))
                .andReturn().getResponse().getContentAsString();
        UUID recordId = recordId(response);
        mvc.perform(get(url(vehicleId) + "/" + recordId)
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Scheduled service"));
        assertThat(vehicles.findById(vehicleId).orElseThrow().getCurrentMileage()).isEqualTo(10500);
    }

    @Test
    void historicalMileageDoesNotDecreaseVehicleAndHigherMileageAdvancesIt() throws Exception {
        Session session = session("maintenance-mileage@example.com");
        UUID vehicleId = vehicle(session, 10000);
        create(session, vehicleId, body(8000, "USD", "INSPECTION",
                "2025-01-01T00:00:00Z")).andExpect(status().isCreated());
        assertThat(vehicles.findById(vehicleId).orElseThrow().getCurrentMileage()).isEqualTo(10000);
        create(session, vehicleId, body(12000, "USD", "GENERAL_SERVICE",
                "2026-01-01T00:00:00Z")).andExpect(status().isCreated());
        assertThat(vehicles.findById(vehicleId).orElseThrow().getCurrentMileage()).isEqualTo(12000);
    }

    @Test
    void historyOrderingIsDeterministic() throws Exception {
        Session session = session("maintenance-order@example.com");
        UUID vehicleId = vehicle(session, 10000);
        UUID old = recordId(create(session, vehicleId, body(9000, "USD", "INSPECTION",
                "2025-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        Thread.sleep(5);
        UUID firstTie = recordId(create(session, vehicleId, body(9500, "USD", "FILTER_CHANGE",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        Thread.sleep(5);
        UUID secondTie = recordId(create(session, vehicleId, body(9600, "USD", "TIRE_SERVICE",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        mvc.perform(get(url(vehicleId)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(secondTie.toString()))
                .andExpect(jsonPath("$.data[1].id").value(firstTie.toString()))
                .andExpect(jsonPath("$.data[2].id").value(old.toString()));
    }

    @Test
    void patchIsPartialAndPreservesOmittedFields() throws Exception {
        Session session = session("maintenance-patch@example.com");
        UUID vehicleId = vehicle(session, 10000);
        UUID recordId = recordId(create(session, vehicleId, body(10000, "USD", "BRAKE_SERVICE",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        mvc.perform(patch(url(vehicleId) + "/" + recordId)
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cost\":75.25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cost").value(75.25))
                .andExpect(jsonPath("$.data.category").value("BRAKE_SERVICE"))
                .andExpect(jsonPath("$.data.title").value("Scheduled service"))
                .andExpect(jsonPath("$.data.currencyCode").value("USD"))
                .andExpect(jsonPath("$.data.serviceProvider").value("Trusted Workshop"));
    }

    @Test
    void softDeleteExcludesAndHidesRecordAndCannotBeRepeated() throws Exception {
        Session session = session("maintenance-delete@example.com");
        UUID vehicleId = vehicle(session, 10000);
        UUID recordId = recordId(create(session, vehicleId, body(10000, "USD", "BATTERY",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        mvc.perform(delete(url(vehicleId) + "/" + recordId)
                        .header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(get(url(vehicleId)).header("Authorization", bearer(session)))
                .andExpect(jsonPath("$.data.length()").value(0));
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                get(url(vehicleId) + "/" + recordId),
                patch(url(vehicleId) + "/" + recordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cost\":1}"),
                delete(url(vehicleId) + "/" + recordId)}) {
            mvc.perform(request.header("Authorization", bearer(session)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("MAINTENANCE_NOT_FOUND"));
        }
        assertThat(maintenanceRecords.findById(recordId).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    void validationRejectsMileageCostCategoryCurrencyAndBlankTitle() throws Exception {
        Session session = session("maintenance-validation@example.com");
        UUID vehicleId = vehicle(session, 10000);
        for (String invalid : new String[] {
                body(-1, "USD", "OIL_CHANGE", "2026-01-01T00:00:00Z"),
                body(10000, "USD", "OIL_CHANGE", "2026-01-01T00:00:00Z")
                        .replace("\"cost\":50.25", "\"cost\":-1"),
                body(10000, "USD", "NOT_A_CATEGORY", "2026-01-01T00:00:00Z"),
                body(10000, "USD", "OIL_CHANGE", "2026-01-01T00:00:00Z")
                        .replace("Scheduled service", "   ")}) {
            create(session, vehicleId, invalid).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        create(session, vehicleId, body(10000, "ZZZ", "OIL_CHANGE",
                "2026-01-01T00:00:00Z")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("CURRENCY_NOT_SUPPORTED"));
    }

    @Test
    void missingAndVehicleMismatchedRecordsAreHidden() throws Exception {
        Session session = session("maintenance-missing@example.com");
        UUID missingVehicle = UUID.randomUUID();
        create(session, missingVehicle, body(1, "USD", "OTHER", "2026-01-01T00:00:00Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        UUID firstVehicle = vehicle(session, 10000);
        UUID secondVehicle = vehicle(session, 10000);
        UUID recordId = recordId(create(session, firstVehicle, body(10000, "USD", "OTHER",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        for (UUID id : new UUID[] {recordId, UUID.randomUUID()}) {
            mvc.perform(get(url(secondVehicle) + "/" + id)
                            .header("Authorization", bearer(session)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("MAINTENANCE_NOT_FOUND"));
        }
    }

    @Test
    void everyCrossUserOperationUsesVehicleNotFoundSemantics() throws Exception {
        Session owner = session("maintenance-owner@example.com");
        Session other = session("maintenance-other@example.com");
        UUID vehicleId = vehicle(owner, 10000);
        UUID recordId = recordId(create(owner, vehicleId, body(10000, "USD", "ENGINE",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        create(other, vehicleId, body(10000, "USD", "ENGINE", "2026-01-01T00:00:00Z"))
                .andExpect(status().isNotFound());
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                get(url(vehicleId)), get(url(vehicleId) + "/" + recordId),
                patch(url(vehicleId) + "/" + recordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cost\":1}"), delete(url(vehicleId) + "/" + recordId)}) {
            mvc.perform(request.header("Authorization", bearer(other)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        }
    }

    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        mvc.perform(post(url(vehicleId)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(1, "USD", "OTHER", "2026-01-01T00:00:00Z")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(url(vehicleId))).andExpect(status().isUnauthorized());
        mvc.perform(get(url(vehicleId) + "/" + recordId)).andExpect(status().isUnauthorized());
        mvc.perform(patch(url(vehicleId) + "/" + recordId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(url(vehicleId) + "/" + recordId)).andExpect(status().isUnauthorized());
    }

    @Test
    void migrationConstraintsAreEnforcedBySqlServer() {
        Integer checkCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.check_constraints
                WHERE name IN ('ck_maintenance_records_category',
                 'ck_maintenance_records_mileage', 'ck_maintenance_records_cost',
                 'ck_maintenance_records_title')
                """, Integer.class);
        Integer indexCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.indexes
                WHERE object_id = OBJECT_ID('maintenance_records')
                 AND name IN ('ix_maintenance_records_vehicle_deleted',
                  'ix_maintenance_records_vehicle_service_date',
                  'ix_maintenance_records_deleted_at')
                """, Integer.class);
        assertThat(checkCount).isEqualTo(4);
        assertThat(indexCount).isEqualTo(3);
    }

    private org.springframework.test.web.servlet.ResultActions create(
            Session session, UUID vehicleId, String body) throws Exception {
        return mvc.perform(post(url(vehicleId)).header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID vehicle(Session session, long mileage) throws Exception {
        String response = mvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"brand":"Test","model":"Car","year":2025,
                         "powertrainType":"GASOLINE","fuelType":"GASOLINE_95",
                         "fuelTankCapacityLiters":50,"currentMileage":%d}
                        """.formatted(mileage)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
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
                        .content("{\"email\":\"%s\",\"password\":\"StrongPass1\"}"
                                .formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = json.readTree(response).path("data");
        return new Session(data.path("accessToken").asText());
    }

    private String body(long mileage, String currency, String category, String serviceDate) {
        String currencyField = currency == null ? "" : "\"currencyCode\":\"" + currency + "\",";
        return """
                {"category":"%s","title":"Scheduled service","serviceDate":"%s",
                 "mileageKm":%d,"cost":50.25,%s"serviceProvider":"Trusted Workshop",
                 "notes":"Completed successfully"}
                """.formatted(category, serviceDate, mileage, currencyField);
    }

    private UUID recordId(String response) throws Exception {
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }
    private String url(UUID vehicleId) {
        return "/api/v1/vehicles/" + vehicleId + "/maintenance-records";
    }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(String token) { }
}
