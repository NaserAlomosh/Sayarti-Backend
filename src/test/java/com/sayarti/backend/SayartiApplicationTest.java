package com.sayarti.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SayartiApplicationTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoadsWithFoundationConfiguration() {
        String encoded = passwordEncoder.encode("a-test-password");
        assertThat(encoded).isNotEqualTo("a-test-password");
        assertThat(passwordEncoder.matches("a-test-password", encoded)).isTrue();
    }
}
