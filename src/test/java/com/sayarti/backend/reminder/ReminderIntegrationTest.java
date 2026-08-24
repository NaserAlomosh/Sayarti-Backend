package com.sayarti.backend.reminder;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReminderIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ReminderRepository reminders;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        reminders.deleteAll();
        vehicles.deleteAll();
        tokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    @Test
    void createsDateAndMileageAndGetsHistoryAndDetails() throws Exception {
        Session session = session("reminders-create@example.com");
        UUID vehicle = vehicle(session);
        UUID date = id(create(session, vehicle, dateBody()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.triggerType").value("DATE"))
                .andReturn().getResponse().getContentAsString());
        create(session, vehicle, mileageBody()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.targetMileage").value(15000));
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get(url(vehicle) + "/" + date).header("Authorization", bearer(session)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Renew"));
    }

    @Test
    void patchIsPartialAndCanChangeBothTriggerDirections() throws Exception {
        Session session = session("reminders-patch@example.com");
        UUID vehicle = vehicle(session);
        UUID reminder = id(create(session, vehicle, dateBody()).andReturn()
                .getResponse().getContentAsString());
        mvc.perform(patch(url(vehicle) + "/" + reminder).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.targetDate").exists());
        mvc.perform(patch(url(vehicle) + "/" + reminder).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"triggerType\":\"MILEAGE\",\"targetMileage\":20000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.targetDate").isEmpty());
        mvc.perform(patch(url(vehicle) + "/" + reminder).header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"triggerType\":\"DATE\",\"targetDate\":\"2027-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.targetMileage").isEmpty());
    }

    @Test
    void completionIsIdempotentAndCompletedReminderRemainsVisible() throws Exception {
        Session session = session("reminders-complete@example.com");
        UUID vehicle = vehicle(session);
        UUID reminder = id(create(session, vehicle, mileageBody()).andReturn()
                .getResponse().getContentAsString());
        String first = complete(session, vehicle, reminder).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true)).andReturn()
                .getResponse().getContentAsString();
        String timestamp = json.readTree(first).path("data").path("completedAt").asText();
        complete(session, vehicle, reminder).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedAt").value(timestamp));
        mvc.perform(get(url(vehicle) + "/" + reminder).header("Authorization", bearer(session)))
                .andExpect(status().isOk());
    }

    @Test
    void softDeleteExcludesAndHidesEverySubsequentOperation() throws Exception {
        Session session = session("reminders-delete@example.com");
        UUID vehicle = vehicle(session);
        UUID reminder = id(create(session, vehicle, dateBody()).andReturn()
                .getResponse().getContentAsString());
        mvc.perform(delete(url(vehicle) + "/" + reminder).header("Authorization", bearer(session)))
                .andExpect(status().isOk());
        mvc.perform(get(url(vehicle)).header("Authorization", bearer(session)))
                .andExpect(jsonPath("$.data.length()").value(0));
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                get(url(vehicle) + "/" + reminder),
                patch(url(vehicle) + "/" + reminder).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"), patch(url(vehicle) + "/" + reminder + "/complete"),
                delete(url(vehicle) + "/" + reminder)}) {
            mvc.perform(request.header("Authorization", bearer(session)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("REMINDER_NOT_FOUND"));
        }
        assertThat(reminders.findById(reminder).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidTargetsBlankTitleNegativeMileageAndEnum() throws Exception {
        Session session = session("reminders-validation@example.com");
        UUID vehicle = vehicle(session);
        for (String body : new String[] {
                "{\"category\":\"CUSTOM\",\"title\":\"x\",\"triggerType\":\"DATE\"}",
                "{\"category\":\"CUSTOM\",\"title\":\"x\",\"triggerType\":\"MILEAGE\"}",
                mileageBody().replace("15000", "-1"), dateBody().replace("Renew", "   "),
                dateBody().replace("CUSTOM", "UNKNOWN")}) {
            create(session, vehicle, body).andExpect(status().is4xxClientError());
        }
        create(session, vehicle, "{\"category\":\"CUSTOM\",\"title\":\"x\","
                + "\"triggerType\":\"DATE\",\"targetMileage\":1}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_REMINDER"));
        create(session, vehicle, mileageBody().replace("OIL_CHANGE", "LICENSE_EXPIRATION"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_REMINDER"));
        create(session, vehicle, mileageBody().replace("OIL_CHANGE", "INSURANCE_EXPIRATION"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_REMINDER"));
    }

    @Test
    void missingMismatchAndEveryCrossUserOperationHideResources() throws Exception {
        Session owner = session("reminders-owner@example.com");
        Session other = session("reminders-other@example.com");
        UUID vehicle = vehicle(owner);
        UUID second = vehicle(owner);
        UUID reminder = id(create(owner, vehicle, dateBody()).andReturn()
                .getResponse().getContentAsString());
        create(owner, UUID.randomUUID(), dateBody()).andExpect(status().isNotFound());
        mvc.perform(get(url(second) + "/" + reminder).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REMINDER_NOT_FOUND"));
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                post(url(vehicle)).contentType(MediaType.APPLICATION_JSON).content(dateBody()),
                get(url(vehicle)), get(url(vehicle) + "/" + reminder),
                patch(url(vehicle) + "/" + reminder).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"), patch(url(vehicle) + "/" + reminder + "/complete"),
                delete(url(vehicle) + "/" + reminder)}) {
            mvc.perform(request.header("Authorization", bearer(other)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
        }
    }

    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        UUID vehicle = UUID.randomUUID();
        UUID reminder = UUID.randomUUID();
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                post(url(vehicle)).contentType(MediaType.APPLICATION_JSON).content(dateBody()),
                get(url(vehicle)), get(url(vehicle) + "/" + reminder),
                patch(url(vehicle) + "/" + reminder).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"), patch(url(vehicle) + "/" + reminder + "/complete"),
                delete(url(vehicle) + "/" + reminder)}) {
            mvc.perform(request).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void migrationConstraintsAndSchedulerIndexesExistInSqlServer() {
        Integer constraints = jdbc.queryForObject("SELECT COUNT(*) FROM sys.check_constraints "
                + "WHERE name IN ('ck_reminders_category','ck_reminders_trigger_type',"
                + "'ck_reminders_title','ck_reminders_target','ck_reminders_completion')",
                Integer.class);
        Integer indexes = jdbc.queryForObject("SELECT COUNT(*) FROM sys.indexes WHERE object_id "
                + "= OBJECT_ID('reminders') AND name IN ('ix_reminders_vehicle_id',"
                + "'ix_reminders_target_date','ix_reminders_target_mileage',"
                + "'ix_reminders_deleted_at','ix_reminders_vehicle_history',"
                + "'ix_reminders_due_date','ix_reminders_due_mileage')", Integer.class);
        assertThat(constraints).isEqualTo(5);
        assertThat(indexes).isEqualTo(7);
    }

    private org.springframework.test.web.servlet.ResultActions create(
            Session session, UUID vehicle, String body) throws Exception {
        return mvc.perform(post(url(vehicle)).header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions complete(
            Session session, UUID vehicle, UUID reminder) throws Exception {
        return mvc.perform(patch(url(vehicle) + "/" + reminder + "/complete")
                .header("Authorization", bearer(session)));
    }

    private UUID vehicle(Session session) throws Exception {
        String response = mvc.perform(post("/api/v1/vehicles").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"brand":"Test","model":"Car","year":2025,
                         "powertrainType":"GASOLINE","fuelType":"GASOLINE_95",
                         "fuelTankCapacityLiters":50,"currentMileage":10000}
                        """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }

    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Reminder","lastName":"Owner","email":"%s",
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

    private String dateBody() {
        return "{\"category\":\"CUSTOM\",\"title\":\"Renew\",\"description\":\"Docs\","
                + "\"triggerType\":\"DATE\",\"targetDate\":\"2027-01-01T00:00:00Z\"}";
    }
    private String mileageBody() {
        return "{\"category\":\"OIL_CHANGE\",\"title\":\"Oil\","
                + "\"triggerType\":\"MILEAGE\",\"targetMileage\":15000}";
    }
    private UUID id(String response) throws Exception {
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }
    private String url(UUID vehicle) { return "/api/v1/vehicles/" + vehicle + "/reminders"; }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(String token) { }
}
