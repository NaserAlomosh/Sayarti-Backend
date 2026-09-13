package com.sayarti.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.sayarti.backend.auth.service.OtpSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class OtpSecurityServiceTest {
    @Test
    void generatesExactlySixDigitsAndStoresOneWayHash() {
        OtpSecurityService service = new OtpSecurityService(new BCryptPasswordEncoder());
        String otp = service.generate();
        String hash = service.hash(otp);
        assertThat(otp).matches("\\d{6}");
        assertThat(hash).doesNotContain(otp);
        assertThat(service.matches(otp, hash)).isTrue();
        assertThat(service.matches("999999", hash)).isFalse();
    }
}
