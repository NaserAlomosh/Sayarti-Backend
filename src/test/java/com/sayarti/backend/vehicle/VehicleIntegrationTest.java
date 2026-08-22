package com.sayarti.backend.vehicle;

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
class VehicleIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired VehicleRepository vehicles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        vehicles.deleteAll(); tokens.deleteAll(); otps.deleteAll(); users.deleteAll();
    }

    @Test
    void createsEverySupportedVehicleForAuthenticatedOwner() throws Exception {
        Session owner = session("owner@example.com");
        create(owner, gasoline()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.powertrainType").value("GASOLINE"));
        create(owner, hybrid()).andExpect(status().isCreated());
        create(owner, plugInHybrid()).andExpect(status().isCreated());
        create(owner, electric()).andExpect(status().isCreated());
        assertThat(vehicles.findAll()).allMatch(v -> v.getUserId().equals(owner.userId()));
    }

    @Test
    void validatesPowertrainConfigurations() throws Exception {
        Session owner = session("validation@example.com");
        invalid(owner, electric().replace("\"batteryCapacityKwh\":75,", ""));
        invalid(owner, electric().replace("\"estimatedRangeKm\":520,", ""));
        create(owner, electric().replace("\"batteryCapacityKwh\":75", "\"batteryCapacityKwh\":0"))
                .andExpect(status().isBadRequest());
        create(owner, electric().replace("\"estimatedRangeKm\":520", "\"estimatedRangeKm\":0"))
                .andExpect(status().isBadRequest());
        invalid(owner, electric().replace("\"powertrainType\":\"ELECTRIC\",",
                "\"powertrainType\":\"ELECTRIC\",\"fuelType\":\"GASOLINE_95\","));
        invalid(owner, electric().replace("\"powertrainType\":\"ELECTRIC\",",
                "\"powertrainType\":\"ELECTRIC\",\"fuelTankCapacityLiters\":40,"));
        invalid(owner, gasoline().replace("\"fuelType\":\"GASOLINE_95\",", ""));
        invalid(owner, gasoline().replace("\"powertrainType\":\"GASOLINE\"",
                "\"powertrainType\":\"DIESEL\""));
        invalid(owner, gasoline().replace("\"powertrainType\":\"GASOLINE\"",
                "\"powertrainType\":\"GASOLINE\",\"batteryCapacityKwh\":2"));
    }

    @Test
    void authenticationOwnershipAndClientUserIdAreEnforced() throws Exception {
        Session owner = session("first@example.com");
        Session other = session("second@example.com");
        mvc.perform(post("/api/v1/vehicles").contentType(MediaType.APPLICATION_JSON)
                .content(gasoline())).andExpect(status().isUnauthorized());
        String payload = gasoline().replace("\"brand\":", "\"userId\":\"" + other.userId()
                + "\",\"brand\":");
        UUID id = id(create(owner, payload).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString());
        assertThat(vehicles.findById(id).orElseThrow().getUserId()).isEqualTo(owner.userId());
        mvc.perform(get("/api/v1/vehicles/{id}", id).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code")
                        .value("VEHICLE_NOT_FOUND"));
        mvc.perform(patch("/api/v1/vehicles/{id}", id).header("Authorization", bearer(other))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"stolen\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/vehicles/{id}/mileage", id)
                .header("Authorization", bearer(other)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mileage\":20000}")) .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/vehicles/{id}", id).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsOnlyOwnedVehiclesAndProtectsInternalFields() throws Exception {
        Session owner = session("list-owner@example.com");
        Session other = session("list-other@example.com");
        UUID id = id(create(owner, gasoline()).andReturn().getResponse().getContentAsString());
        create(other, electric());
        mvc.perform(get("/api/v1/vehicles").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()));
        mvc.perform(get("/api/v1/vehicles/{id}", id).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/vehicles/{id}", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nickname":"Updated","currentMileage":0,"userId":"%s",
                         "id":"00000000-0000-0000-0000-000000000000","deletedAt":"2020-01-01"}
                        """.formatted(other.userId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("Updated"))
                .andExpect(jsonPath("$.data.currentMileage").value(10000));
        assertThat(vehicles.findById(id).orElseThrow().getUserId()).isEqualTo(owner.userId());
    }

    @Test
    void mileageAndSoftDeleteFollowDomainRules() throws Exception {
        Session owner = session("delete@example.com");
        UUID id = id(create(owner, gasoline()).andReturn().getResponse().getContentAsString());
        mvc.perform(patch("/api/v1/vehicles/{id}/mileage", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"mileage\":12000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.currentMileage").value(12000));
        mvc.perform(patch("/api/v1/vehicles/{id}/mileage", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"mileage\":11000}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error.code")
                        .value("INVALID_VEHICLE_MILEAGE"));
        mvc.perform(delete("/api/v1/vehicles/{id}", id).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        assertThat(vehicles.findById(id).orElseThrow().getDeletedAt()).isNotNull();
        mvc.perform(get("/api/v1/vehicles").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/v1/vehicles/{id}", id).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions create(Session session, String body)
            throws Exception {
        return mvc.perform(post("/api/v1/vehicles").header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private void invalid(Session session, String body) throws Exception {
        create(session, body).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_VEHICLE_CONFIGURATION"));
    }
    private UUID id(String response) throws Exception {
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }
    private String bearer(Session session) { return "Bearer " + session.accessToken(); }

    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Vehicle","lastName":"Owner","email":"%s","password":"StrongPass1","countryCode":"JO"}
                """.formatted(email))).andExpect(status().isCreated());
        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElseThrow();
        user.verifyEmail(); users.saveAndFlush(user);
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"StrongPass1\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = json.readTree(response).path("data");
        return new Session(user.getId(), data.path("accessToken").asText());
    }
    private String gasoline() { return """
            {"brand":"Lincoln","model":"MKZ","year":2013,"powertrainType":"GASOLINE",
             "fuelType":"GASOLINE_95","fuelTankCapacityLiters":66,"currentMileage":10000}
            """; }
    private String hybrid() { return """
            {"brand":"Toyota","model":"Prius","year":2024,"powertrainType":"HYBRID",
             "fuelType":"GASOLINE_95","fuelTankCapacityLiters":43,"batteryCapacityKwh":1.3,
             "currentMileage":25000}
            """; }
    private String plugInHybrid() { return """
            {"brand":"Toyota","model":"Prius Prime","year":2025,
             "powertrainType":"PLUG_IN_HYBRID","fuelType":"GASOLINE_95",
             "fuelTankCapacityLiters":40,"batteryCapacityKwh":13.6,"estimatedRangeKm":70,
             "currentMileage":12000}
            """; }
    private String electric() { return """
            {"brand":"Tesla","model":"Model 3","year":2025,"powertrainType":"ELECTRIC",
             "batteryCapacityKwh":75,"estimatedRangeKm":520,"currentMileage":18000}
            """; }
    private record Session(UUID userId, String accessToken) { }
}
