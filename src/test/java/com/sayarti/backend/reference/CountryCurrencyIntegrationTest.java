package com.sayarti.backend.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.AbstractIntegrationTest;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.security.oauth.GoogleIdentity;
import com.sayarti.backend.security.oauth.GoogleTokenVerifier;
import com.sayarti.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CountryCurrencyIntegrationTest extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository tokens;
    @MockitoBean GoogleTokenVerifier googleTokens;

    @BeforeEach void clearUsers() { tokens.deleteAll(); users.deleteAll(); }

    @Test void referenceDataIsPublicOrderedAndMapped() throws Exception {
        mvc.perform(get("/api/v1/reference/countries")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("AE"))
                .andExpect(jsonPath("$.data[?(@.code == 'JO')].name").value("Jordan"))
                .andExpect(jsonPath("$.data[?(@.code == 'JO')].flag").value("🇯🇴"))
                .andExpect(jsonPath("$.data[?(@.code == 'JO')].currencyCode").value("JOD"))
                .andExpect(jsonPath("$.data[?(@.code == 'JO')].defaultCurrencyCode").value("JOD"))
                .andExpect(jsonPath("$.data[?(@.code == 'AE')].defaultCurrencyCode").value("AED"));
        mvc.perform(get("/api/v1/reference/currencies")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("AED"))
                .andExpect(jsonPath("$.data[?(@.code == 'JOD')].name").value("Jordanian Dinar"))
                .andExpect(jsonPath("$.data[?(@.code == 'JOD')].symbol").value("JD"))
                .andExpect(jsonPath("$.data[?(@.code == 'USD')].decimalDigits").value(2));
    }

    @Test void referenceDisplayValuesAreArabicButIsoIdentifiersRemainStable() throws Exception {
        mvc.perform(get("/api/v1/reference/countries").header("Accept-Language", "ar-JO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'JO')].code").value("JO"))
                .andExpect(jsonPath("$.data[?(@.code == 'JO')].name").value("الأردن"))
                .andExpect(jsonPath("$.data[?(@.code == 'JO')].flag").value("🇯🇴"))
                .andExpect(jsonPath("$.data[?(@.code == 'JO')].currencyCode").value("JOD"));
        mvc.perform(get("/api/v1/reference/currencies").header("Accept-Language", "ar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'JOD')].code").value("JOD"))
                .andExpect(jsonPath("$.data[?(@.code == 'JOD')].name").value("الدينار الأردني"))
                .andExpect(jsonPath("$.data[?(@.code == 'JOD')].symbol").value("د.أ"));
    }

    @Test void registrationAssignsCountryCurrencyAndRejectsUnsupportedCountry() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registration("jo@example.com", "JO"))).andExpect(status().isCreated());
        var user = users.findByEmailIgnoreCaseAndDeletedAtIsNull("jo@example.com").orElseThrow();
        assertThat(user.getCountryCode()).isEqualTo("JO");
        assertThat(user.getDefaultCurrencyCode()).isEqualTo("JOD");
        assertThat(user.isEmailVerified()).isFalse();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registration("bad@example.com", "ZZ"))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("COUNTRY_NOT_SUPPORTED"));
    }

    @Test void googleOnboardingSelectsCountryThenAllowsIndependentCurrencyChange() throws Exception {
        when(googleTokens.verify("google-token")).thenReturn(new GoogleIdentity(
                "subject-1", "google@example.com", "Google", "User"));
        String response = mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"google-token\"}" )).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiredAction").value("SELECT_COUNTRY"))
                .andReturn().getResponse().getContentAsString();
        String access = json.readTree(response).at("/data/accessToken").asText();
        mvc.perform(patch("/api/v1/users/me/country").header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"countryCode\":\"JO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.countryCode").value("JO"))
                .andExpect(jsonPath("$.data.defaultCurrencyCode").value("JOD"));
        mvc.perform(patch("/api/v1/users/me/default-currency").header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"currencyCode\":\"USD\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.countryCode").value("JO"))
                .andExpect(jsonPath("$.data.defaultCurrencyCode").value("USD"));
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"google-token\"}" )).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiredAction").doesNotExist());
    }

    private String registration(String email, String country) {
        return """
                {"firstName":"Test","lastName":"User","email":"%s",
                 "password":"StrongPass1","countryCode":"%s",
                 "defaultCurrencyCode":"USD"}
                """.formatted(email, country);
    }
}
