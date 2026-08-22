package com.sayarti.backend.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sayarti.backend.security.filter.JwtAuthenticationFilter;
import com.sayarti.backend.security.jwt.JwtService;
import com.sayarti.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
        useDefaultFilters = false,
        properties = "sayarti.cors.allowed-origins=http://localhost")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityConfigTest.TestEndpointConfiguration.class
})
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

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

    @TestConfiguration(proxyBeanMethods = false)
    static class TestEndpointConfiguration {
        @Bean
        TestEndpoint testEndpoint() {
            return new TestEndpoint();
        }
    }

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
