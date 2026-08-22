package com.sayarti.backend.user;

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
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.user.entity.AuthProvider;
import com.sayarti.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL = "profile@example.com";
    private static final String PASSWORD = "StrongPass1";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UserRepository users;

    @Autowired
    RefreshTokenRepository tokens;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clear() {
        tokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void getsCurrentUserWithoutExposingSecurityFields() throws Exception {
        Session session = register();

        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(session.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Sara"))
                .andExpect(jsonPath("$.data.lastName").value("Ali"))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.googleSubject").doesNotExist())
                .andExpect(jsonPath("$.data.refreshTokens").doesNotExist())
                .andExpect(jsonPath("$.data.deletedAt").doesNotExist());
    }

    @Test
    void rejectsUnauthenticatedProfileAccess() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Maya\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatesFirstName() throws Exception {
        Session session = register();

        mvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"  Maya  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Maya"))
                .andExpect(jsonPath("$.data.lastName").value("Ali"));
    }

    @Test
    void updatesLastName() throws Exception {
        Session session = register();

        mvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lastName\":\"Hassan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Sara"))
                .andExpect(jsonPath("$.data.lastName").value("Hassan"));
    }

    @Test
    void validatesProfileUpdates() throws Exception {
        Session session = register();

        mvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cannotUpdateProtectedOrInternalFieldsThroughProfileDto() throws Exception {
        Session session = register();
        var before = users.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL).orElseThrow();
        String passwordHash = before.getPasswordHash();

        mvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "Maya",
                                      "email": "attacker@example.com",
                                      "passwordHash": "plain-text",
                                      "authProvider": "GOOGLE",
                                      "googleSubject": "attacker-subject",
                                      "id": "00000000-0000-0000-0000-000000000000",
                                      "deletedAt": "2020-01-01T00:00:00Z"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Maya"))
                .andExpect(jsonPath("$.data.email").value(EMAIL));

        var after = users.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL).orElseThrow();
        assertThat(after.getId()).isEqualTo(before.getId());
        assertThat(after.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(after.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(after.getGoogleSubject()).isNull();
        assertThat(after.getDeletedAt()).isNull();
        assertThat(users.findByEmailIgnoreCaseAndDeletedAtIsNull("attacker@example.com")).isEmpty();
    }

    @Test
    void deletesAccountRevokesAllSessionsAndPreventsAuthentication() throws Exception {
        Session registration = register();
        Session login = login();

        mvc.perform(delete("/api/v1/users/me")
                            .header("Authorization", bearer(registration.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        assertThat(users.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).isEmpty();
        Integer activeTokens = jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE revoked_at IS NULL", Integer.class);
        assertThat(activeTokens).isZero();

        mvc.perform(get("/api/v1/users/me")
                            .header("Authorization", bearer(login.accessToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));

        mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));

        mvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"%s\"}"
                                    .formatted(login.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REUSED"));
    }

    private Session register() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content("""
                                                       {
                                                         "firstName": "Sara",
                                                         "lastName": "Ali",
                                                         "email": "%s",
                                                         "password": "%s",
                              "countryCode": "JO"
                                                       }
                                                       """.formatted(EMAIL, PASSWORD)))
                                   .andExpect(status().isCreated())
                                   .andReturn();
        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL).orElseThrow();
        user.verifyEmail();
        users.saveAndFlush(user);
        return login();
    }

    private Session login() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content(loginRequest()))
                                   .andExpect(status().isOk())
                                   .andReturn();
        return session(result);
    }

    private String loginRequest() {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(EMAIL, PASSWORD);
    }

    private Session session(MvcResult result) throws Exception {
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return new Session(
                body.at("/data/accessToken").asText(),
                body.at("/data/refreshToken").asText());
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record Session(String accessToken, String refreshToken) {
    }
}
