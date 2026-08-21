package com.sayarti.backend.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sayarti.backend.SayartiApplication;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.auth.repository.EmailVerificationOtpRepository;
import com.sayarti.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        classes = SayartiApplication.class,
        properties =
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jpa."
                        + "JpaRepositoriesAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigTest.TestEndpoint.class)
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private EmailVerificationOtpRepository emailVerificationOtpRepository;

    @Test
    void protectsNonPublicRoutesWithStandardError() throws Exception {
        mockMvc.perform(get("/test/secured"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void permitsDocumentedAuthenticationRoute() throws Exception {
        mockMvc.perform(get("/api/v1/auth/ping")).andExpect(status().isOk());
    }

    @TestConfiguration
    @RestController
    static class TestEndpoint {
        @GetMapping("/test/secured")
        String secured() {
            return "secured";
        }

        @GetMapping("/api/v1/auth/ping")
        String publicAuth() {
            return "public";
        }
    }
}
