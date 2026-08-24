package com.sayarti.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sayarti.backend.AbstractIntegrationTest;
import com.sayarti.backend.auth.repository.EmailVerificationOtpRepository;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.user.repository.UserRepository;
import java.time.Instant;
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
class EmailVerificationIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired EmailVerificationOtpRepository otps;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clear() {
        refreshTokens.deleteAll();
        otps.deleteAll();
        users.deleteAll();
    }

    @Test
    void registrationStoresOnlyHashAndValidOtpCreatesSessionOnce() throws Exception {
        register("verify@example.com");
        String plaintext = testEmailService.latestOtpFor("verify@example.com");
        var otp = otps.findAll().get(0);

        assertThat(otp.getOtpHash()).doesNotContain(plaintext);

        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull("verify@example.com")
                .orElseThrow();

        assertThat(user.isEmailVerified()).isFalse();

        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"verify@example.com\",\"otp\":\"%s\"}"
                                .formatted(plaintext)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        assertThat(users.findByEmailIgnoreCaseAndDeletedAtIsNull("verify@example.com")
                .orElseThrow().isEmailVerified()).isTrue();

        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"verify@example.com\",\"otp\":\"%s\"}"
                                .formatted(plaintext)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_ALREADY_VERIFIED"));
    }

    @Test
    void invalidAttemptsIncrementAndReachConfiguredLimit() throws Exception {
        register("attempts@example.com");
        for (int i = 1; i <= 4; i++) {
            mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"attempts@example.com\",\"otp\":\"000000\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("AUTH_OTP_INVALID"));
            assertThat(otps.findAll().get(0).getAttemptCount()).isEqualTo(i);
        }
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"attempts@example.com\",\"otp\":\"000000\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AUTH_OTP_ATTEMPTS_EXCEEDED"));
    }

    @Test
    void resendEnforcesCooldownWithoutReplacingActiveOtp() throws Exception {
        register("resend@example.com");
        var original = otps.findAll().get(0);
        mvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"resend@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AUTH_OTP_RESEND_TOO_SOON"));
        assertThat(otps.findAll()).singleElement().extracting("id").isEqualTo(original.getId());
    }

    @Test
    void expiredAndInvalidatedOtpsAreRejectedWithStableCodes() throws Exception {
        register("expired@example.com");
        String expiredCode = testEmailService.latestOtpFor("expired@example.com");
        var expired = otps.findAll().get(0);
        jdbc.update("UPDATE email_verification_otps SET expires_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)), expired.getId());
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"expired@example.com\",\"otp\":\"%s\"}"
                                .formatted(expiredCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_OTP_EXPIRED"));

        register("invalidated@example.com");
        String invalidatedCode = testEmailService.latestOtpFor("invalidated@example.com");
        var invalidated = otps.findAll().stream()
                .filter(otp -> otp.getUser().getEmail().equals("invalidated@example.com"))
                .findFirst().orElseThrow();
        invalidated.invalidate(Instant.now());
        otps.saveAndFlush(invalidated);
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalidated@example.com\",\"otp\":\"%s\"}"
                                .formatted(invalidatedCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_OTP_INVALID"));
    }

    @Test
    void resendAfterCooldownInvalidatesPreviousOtpAndOnlyReplacementWorks() throws Exception {
        register("replacement@example.com");
        String originalCode = testEmailService.latestOtpFor("replacement@example.com");
        var original = otps.findAll().get(0);
        jdbc.update("UPDATE email_verification_otps SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(61)), original.getId());

        mvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"replacement@example.com\"}"))
                .andExpect(status().isOk());
        String replacementCode = testEmailService.latestOtpFor("replacement@example.com");
        assertThat(replacementCode).isNotEqualTo(originalCode);
        assertThat(otps.findById(original.getId()).orElseThrow().getInvalidatedAt()).isNotNull();

        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"replacement@example.com\",\"otp\":\"%s\"}"
                                .formatted(originalCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_OTP_INVALID"));
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"replacement@example.com\",\"otp\":\"%s\"}"
                                .formatted(replacementCode)))
                .andExpect(status().isOk());
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Sara","lastName":"Ali","email":"%s","password":"StrongPass1","countryCode":"JO"}
                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.verificationRequired").value(true))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

}
