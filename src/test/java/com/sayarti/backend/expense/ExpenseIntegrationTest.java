package com.sayarti.backend.expense;

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
import com.sayarti.backend.expense.repository.ExpenseRepository;
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
class ExpenseIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ExpenseRepository expenses;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        expenses.deleteAll();
        vehicles.deleteAll();
        tokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    @Test
    void createsGetsAndUsesDefaultCurrency() throws Exception {
        Session session = session("expense-create@example.com");
        UUID vehicleId = vehicle(session, 10000);
        String response = create(session, vehicleId, body(10500, null, "FUEL",
                "2026-08-20T10:00:00Z")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data.category").value("FUEL"))
                .andReturn().getResponse().getContentAsString();
        UUID recordId = recordId(response);
        mvc.perform(get(url(vehicleId) + "/" + recordId)
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Road expense"));
    }

    @Test
    void historyOrderingIsDeterministic() throws Exception {
        Session session = session("expense-order@example.com");
        UUID vehicleId = vehicle(session, 10000);
        UUID old = recordId(create(session, vehicleId, body(9000, "USD", "INSURANCE",
                "2025-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        Thread.sleep(5);
        UUID firstTie = recordId(create(session, vehicleId, body(9500, "USD", "PARKING",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        Thread.sleep(5);
        UUID secondTie = recordId(create(session, vehicleId, body(9600, "USD", "TOLL",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        mvc.perform(get(url(vehicleId)).header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(secondTie.toString()))
                .andExpect(jsonPath("$.data[1].id").value(firstTie.toString()))
                .andExpect(jsonPath("$.data[2].id").value(old.toString()));
    }

    @Test
    void patchIsPartialAndPreservesOmittedFields() throws Exception {
        Session session = session("expense-patch@example.com");
        UUID vehicleId = vehicle(session, 10000);
        UUID recordId = recordId(create(session, vehicleId, body(10000, "USD", "REPAIR",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        mvc.perform(patch(url(vehicleId) + "/" + recordId)
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":75.25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(75.25))
                .andExpect(jsonPath("$.data.category").value("REPAIR"))
                .andExpect(jsonPath("$.data.title").value("Road expense"))
                .andExpect(jsonPath("$.data.currencyCode").value("USD")));
    }

    @Test
    void softDeleteExcludesAndHidesRecordAndCannotBeRepeated() throws Exception {
        Session session = session("expense-delete@example.com");
        UUID vehicleId = vehicle(session, 10000);
        UUID recordId = recordId(create(session, vehicleId, body(10000, "USD", "ACCESSORY",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        mvc.perform(delete(url(vehicleId) + "/" + recordId)
                        .header("Authorization", bearer(session))).andExpect(status().isOk());
        mvc.perform(get(url(vehicleId)).header("Authorization", bearer(session)))
                .andExpect(jsonPath("$.data.length()").value(0));
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                get(url(vehicleId) + "/" + recordId),
                patch(url(vehicleId) + "/" + recordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1}"),
                delete(url(vehicleId) + "/" + recordId)}) {
            mvc.perform(request.header("Authorization", bearer(session)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("EXPENSE_NOT_FOUND"));
        }
        assertThat(expenses.findById(recordId).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    void validationRejectsAmountCategoryCurrencyAndBlankTitle() throws Exception {
        Session session = session("expense-validation@example.com");
        UUID vehicleId = vehicle(session, 10000);
        for (String invalid : new String[] {
                body(1, "USD", "FUEL", "2026-01-01T00:00:00Z").replace("50.25", "0"),
                body(10000, "USD", "FUEL", "2026-01-01T00:00:00Z")
                        .replace("50.25", "-1"),
                body(10000, "USD", "NOT_A_CATEGORY", "2026-01-01T00:00:00Z"),
                body(10000, "USD", "FUEL", "2026-01-01T00:00:00Z")
                        .replace("Road expense", "   ")}) {
            create(session, vehicleId, invalid).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        create(session, vehicleId, body(10000, "ZZZ", "FUEL",
                "2026-01-01T00:00:00Z")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("CURRENCY_NOT_SUPPORTED"));
    }

    @Test
    void missingAndVehicleMismatchedRecordsAreHidden() throws Exception {
        Session session = session("expense-missing@example.com");
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
                    .andExpect(jsonPath("$.error.code").value("EXPENSE_NOT_FOUND"));
        }
    }

    @Test
    void everyCrossUserOperationUsesVehicleNotFoundSemantics() throws Exception {
        Session owner = session("expense-owner@example.com");
        Session other = session("expense-other@example.com");
        UUID vehicleId = vehicle(owner, 10000);
        UUID recordId = recordId(create(owner, vehicleId, body(10000, "USD", "WASH",
                "2026-01-01T00:00:00Z")).andReturn().getResponse().getContentAsString());
        create(other, vehicleId, body(10000, "USD", "WASH", "2026-01-01T00:00:00Z"))
                .andExpect(status().isNotFound());
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                get(url(vehicleId)), get(url(vehicleId) + "/" + recordId),
                patch(url(vehicleId) + "/" + recordId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1}"), delete(url(vehicleId) + "/" + recordId)}) {
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
                WHERE name IN ('ck_expenses_category','ck_expenses_amount','ck_expenses_title')
                """, Integer.class);
        Integer indexCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.indexes
                WHERE object_id = OBJECT_ID('expenses')
                 AND name IN ('ix_expenses_vehicle_id','ix_expenses_expense_date','ix_expenses_category',
                  'ix_expenses_deleted_at','ix_expenses_vehicle_history')
                """, Integer.class);
        assertThat(checkCount).isEqualTo(3);
        assertThat(indexCount).isEqualTo(5);
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
                {"firstName":"Expense","lastName":"Owner","email":"%s",
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

    private String body(long ignored, String currency, String category, String expenseDate) {
        String currencyField = currency == null ? "" : "\"currencyCode\":\"" + currency + "\",";
        return """
                {"category":"%s","title":"Road expense","expenseDate":"%s",
                 "amount":50.25,%s"notes":"Paid in full"}
                """.formatted(category, expenseDate, currencyField);
    }

    private UUID recordId(String response) throws Exception {
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }
    private String url(UUID vehicleId) {
        return "/api/v1/vehicles/" + vehicleId + "/expenses";
    }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(String token) { }
}
