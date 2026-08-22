package com.sayarti.backend.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.AbstractIntegrationTest;
import com.sayarti.backend.auth.repository.EmailVerificationOtpRepository;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.device.entity.Device;
import com.sayarti.backend.device.entity.DevicePlatform;
import com.sayarti.backend.device.repository.DeviceRepository;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired DeviceRepository devices;
    @Autowired RefreshTokenRepository tokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach @AfterEach
    void clearDatabase() {
        devices.deleteAll();
        tokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    @Test
    void registerIsIdempotentAndHandlesRotation() throws Exception {
        Session session = session("register-device@example.com");
        String first = register(session, body("stable", "ANDROID", "token-one"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.fcmToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        UUID id = id(first);
        register(session, body("stable", "IOS", "token-two")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.platform").value("IOS"));
        update(session, id, "token-three").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
        assertThat(devices.count()).isOne();
        Device device = devices.findById(id).orElseThrow();
        assertThat(device.getFcmToken()).isEqualTo("token-three");
        assertThat(device.getUpdatedAt()).isAfterOrEqualTo(device.getCreatedAt());
    }

    @Test
    void deletePreventsSubsequentUpdateAndMissingIsHidden() throws Exception {
        Session session = session("delete-device@example.com");
        UUID id = id(register(session, body("phone", "ANDROID", "delete-token"))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(delete("/api/v1/devices/" + id).header("Authorization", bearer(session)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.deleted").value(true));
        update(session, id, "later").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEVICE_NOT_FOUND"));
        update(session, UUID.randomUUID(), "missing").andExpect(status().isNotFound());
    }

    @Test
    void crossUserUpdateAndDeleteAreHidden() throws Exception {
        Session owner = session("device-owner@example.com");
        Session other = session("device-other@example.com");
        UUID id = id(register(owner, body("phone", "IOS", "owner-token"))
                .andReturn().getResponse().getContentAsString());
        update(other, id, "stolen").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEVICE_NOT_FOUND"));
        mvc.perform(delete("/api/v1/devices/" + id).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEVICE_NOT_FOUND"));
        assertThat(devices.findById(id)).isPresent();
    }

    @Test
    void allEndpointsRequireAuthentication() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/api/v1/devices").contentType(MediaType.APPLICATION_JSON)
                .content(body("phone", "ANDROID", "token"))).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/devices/" + id + "/fcm-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"fcmToken\":\"token\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/devices/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void validatesPlatformIdentifierAndTokens() throws Exception {
        Session session = session("device-validation@example.com");
        for (String request : new String[] {body("phone", "WINDOWS", "token"),
                body("   ", "ANDROID", "token"), body("phone", "ANDROID", "   ")}) {
            register(session, request).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        UUID id = id(register(session, body("phone", "ANDROID", "valid"))
                .andReturn().getResponse().getContentAsString());
        update(session, id, "   ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void reassignedTokenRemovesStaleRegistrationAndPreventsDuplicateDelivery() throws Exception {
        Session oldOwner = session("old-device@example.com");
        Session newOwner = session("new-device@example.com");
        UUID stale = id(register(oldOwner, body("old", "ANDROID", "shared"))
                .andReturn().getResponse().getContentAsString());
        UUID retained = id(register(newOwner, body("new", "IOS", "temporary"))
                .andReturn().getResponse().getContentAsString());
        update(newOwner, retained, "shared").andExpect(status().isOk());
        assertThat(devices.findById(stale)).isEmpty();
        assertThat(devices.findById(retained).orElseThrow().getFcmToken()).isEqualTo("shared");
        assertThat(devices.count()).isOne();
    }

    @Test
    void databaseUniquenessAndMigrationObjectsAreVerified() {
        User one = users.save(new User("One", "User", "db-one@example.com", "hash"));
        User two = users.save(new User("Two", "User", "db-two@example.com", "hash"));
        devices.saveAndFlush(new Device(one.getId(), "logical", DevicePlatform.ANDROID, "first"));
        assertThatThrownBy(() -> devices.saveAndFlush(
                new Device(one.getId(), "logical", DevicePlatform.IOS, "second")))
                .isInstanceOf(DataIntegrityViolationException.class);
        devices.deleteAll();
        devices.saveAndFlush(new Device(one.getId(), "one", DevicePlatform.ANDROID, "duplicate"));
        assertThatThrownBy(() -> devices.saveAndFlush(
                new Device(two.getId(), "two", DevicePlatform.IOS, "duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
        Integer checks = jdbc.queryForObject("SELECT COUNT(*) FROM sys.check_constraints WHERE "
                + "name IN ('ck_devices_identifier','ck_devices_platform','ck_devices_fcm_token')",
                Integer.class);
        Integer indexes = jdbc.queryForObject("SELECT COUNT(*) FROM sys.indexes WHERE object_id "
                + "= OBJECT_ID('devices') AND name IN ('ix_devices_user_id',"
                + "'ix_devices_fcm_token','ix_devices_logical_identity',"
                + "'uq_devices_user_identifier','uq_devices_fcm_token')", Integer.class);
        assertThat(checks).isEqualTo(3);
        assertThat(indexes).isEqualTo(5);
    }

    private ResultActions register(Session session, String content) throws Exception {
        return mvc.perform(post("/api/v1/devices").header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON).content(content));
    }
    private ResultActions update(Session session, UUID id, String token) throws Exception {
        return mvc.perform(patch("/api/v1/devices/" + id + "/fcm-token")
                .header("Authorization", bearer(session)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"%s\"}".formatted(token)));
    }
    private Session session(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Device","lastName":"Owner","email":"%s",
                 "password":"StrongPass1","countryCode":"JO"}
                """.formatted(email))).andExpect(status().isCreated());
        User user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElseThrow();
        user.verifyEmail();
        users.saveAndFlush(user);
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"StrongPass1\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Session(json.readTree(response).path("data").path("accessToken").asText());
    }
    private String body(String identifier, String platform, String token) {
        return "{\"deviceIdentifier\":\"%s\",\"platform\":\"%s\",\"fcmToken\":\"%s\"}"
                .formatted(identifier, platform, token);
    }
    private UUID id(String response) throws Exception {
        return UUID.fromString(json.readTree(response).path("data").path("id").asText());
    }
    private String bearer(Session session) { return "Bearer " + session.token(); }
    private record Session(String token) { }
}
