package com.sayarti.backend.email;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestEmailConfiguration {
    @Bean
    @Primary
    TestEmailService testEmailService() {
        return new TestEmailService();
    }
}
