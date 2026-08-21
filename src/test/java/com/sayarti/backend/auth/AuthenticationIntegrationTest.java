package com.sayarti.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    @MockitoBean
    GoogleTokenVerifier googleTokens;

    @BeforeEach
    void clear() {
        tokens.deleteAll();
        users.deleteAll();
    }

    private JsonNode register(String email) throws Exception {
        return body(mvc.perform(post("/api/v1/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                            {
                              "firstName": "Sara",
                              "lastName": "Ali",
                              "email": "%s",
                              "password": "StrongPass1"
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

        assertThat(response.at("/data/user/email").asText()).isEqualTo("sara@example.com");

        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull("sara@example.com").orElseThrow();

        assertThat(user.getPasswordHash()).doesNotContain("StrongPass1");

        assertThat(encoder.matches("StrongPass1", user.getPasswordHash())).isTrue();
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
                          "password": "StrongPass1"
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
                          "password": "weak"
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
}
