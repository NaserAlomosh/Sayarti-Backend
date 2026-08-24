package com.sayarti.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.AbstractIntegrationTest;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.security.oauth.GoogleAuthenticationException;
import com.sayarti.backend.security.oauth.GoogleIdentity;
import com.sayarti.backend.security.oauth.GoogleTokenVerifier;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UserRepository users;

    @Autowired
    RefreshTokenRepository tokens;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    GoogleTokenVerifier googleTokens;

    private JsonNode register(String email) throws Exception {
        return body(mvc.perform(post("/api/v1/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                            {
                              "firstName": "Sara",
                              "lastName": "Ali",
                              "email": "%s",
                              "password": "StrongPass1",
                              "countryCode": "JO"
                            }
                            """.formatted(email)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true))
                        .andReturn());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void registersAndHashesPassword() throws Exception {
        JsonNode response = register("Sara@Example.com");

        assertThat(response.at("/data/email").asText()).isEqualTo("sara@example.com");
        assertThat(response.at("/data/verificationRequired").asBoolean()).isTrue();
        assertThat(response.at("/data/accessToken").isMissingNode()).isTrue();

        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull("sara@example.com").orElseThrow();

        assertThat(user.getPasswordHash()).doesNotContain("StrongPass1");

        assertThat(encoder.matches("StrongPass1", user.getPasswordHash())).isTrue();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void mapsAllPersistedInstantsToUtcAwareSqlServerColumns() {
        var timestampColumns = jdbc.queryForList("""
                SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATETIME_PRECISION
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE (TABLE_NAME = 'users'
                        AND COLUMN_NAME IN ('created_at', 'updated_at', 'deleted_at'))
                   OR (TABLE_NAME = 'refresh_tokens'
                        AND COLUMN_NAME IN ('expires_at', 'created_at', 'revoked_at'))
                """);

        assertThat(timestampColumns).hasSize(6);
        assertThat(timestampColumns)
                .allSatisfy(column -> {
                    assertThat(column.get("DATA_TYPE")).isEqualTo("datetimeoffset");
                    assertThat(((Number) column.get("DATETIME_PRECISION")).intValue()).isEqualTo(6);
                });
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        register("same@example.com");

        mvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                        {
                          "firstName": "A",
                          "lastName": "B",
                          "email": "SAME@example.com",
                          "password": "StrongPass1",
                          "countryCode": "JO"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void validatesRegistrationDto() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                        {
                          "firstName": "",
                          "lastName": "B",
                          "email": "bad",
                          "password": "weak",
                              "countryCode": "JO"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void logsInAndRejectsBadCredentials() throws Exception {
        register("login@example.com");

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "email": "login@example.com",
                          "password": "StrongPass1"
                        }
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_NOT_VERIFIED"));

        User user = users.findByEmailIgnoreCaseAndDeletedAtIsNull("login@example.com").orElseThrow();
        user.verifyEmail();
        users.saveAndFlush(user);

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "email": "login@example.com",
                          "password": "StrongPass1"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "email": "login@example.com",
                          "password": "wrong"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void rotatesRefreshTokensStoresOnlyHashesAndRejectsReuse() throws Exception {
        register("rotation@example.com");
        User user = users.findByEmailIgnoreCaseAndDeletedAtIsNull("rotation@example.com")
                .orElseThrow();
        user.verifyEmail();
        users.saveAndFlush(user);

        JsonNode login = body(mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"rotation@example.com","password":"StrongPass1"}
                                """))
                .andExpect(status().isOk()).andReturn()).path("data");
        String original = login.path("refreshToken").asText();
        assertThat(jdbc.queryForList("SELECT token_hash FROM refresh_tokens", String.class))
                .noneMatch(original::equals);

        JsonNode rotated = body(mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(original)))
                .andExpect(status().isOk()).andReturn()).path("data");
        assertThat(rotated.path("refreshToken").asText()).isNotEqualTo(original);
        assertThat(tokens.findAll()).hasSize(2);
        assertThat(tokens.findAll()).filteredOn(token -> token.getRevokedAt() != null)
                .hasSize(1);

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(original)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REUSED"));
    }

    @Test
    void logoutRevokesRefreshTokenAndPreventsFurtherUse() throws Exception {
        register("logout@example.com");
        User user = users.findByEmailIgnoreCaseAndDeletedAtIsNull("logout@example.com")
                .orElseThrow();
        user.verifyEmail();
        users.saveAndFlush(user);
        String refreshToken = body(mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"logout@example.com\",\"password\":\"StrongPass1\"}"))
                .andExpect(status().isOk()).andReturn()).at("/data/refreshToken").asText();

        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revoked").value(true));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REUSED"));
    }

    @Test
    void googleAuthenticationCreatesAndReusesVerifiedSubjectButNeverLinksLocalEmail()
            throws Exception {
        when(googleTokens.verify("new-google-token")).thenReturn(new GoogleIdentity(
                "google-subject", "google@example.com", "Google", "User"));
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"new-google-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.emailVerified").value(true));
        assertThat(users.count()).isOne();

        when(googleTokens.verify("returning-google-token")).thenReturn(new GoogleIdentity(
                "google-subject", "google@example.com", "Updated", "Name"));
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"returning-google-token\"}"))
                .andExpect(status().isOk());
        assertThat(users.count()).isOne();

        register("local@example.com");
        when(googleTokens.verify("collision-token")).thenReturn(new GoogleIdentity(
                "different-subject", "local@example.com", "Local", "Collision"));
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"collision-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_ACCOUNT_LINKING_REQUIRED"));
    }
}
