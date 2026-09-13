package com.sayarti.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sayarti.autoconfigure.notification.NotificationProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class NotificationProviderConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationProviderAutoConfiguration.class));

    @Test
    void registersUnavailableProviderWhenNoDeliveryProviderExists() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NotificationProvider.class);
            assertThat(context.getBean(NotificationProvider.class))
                    .isInstanceOf(UnavailableNotificationProvider.class);
        });
    }

    @Test
    void backsOffWhenAnotherProviderExists() {
        contextRunner.withUserConfiguration(AvailableProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationProvider.class);
                    assertThat(context.getBean(NotificationProvider.class))
                            .isSameAs(context.getBean("availableNotificationProvider"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class AvailableProviderConfiguration {
        @Bean
        NotificationProvider availableNotificationProvider() {
            return mock(NotificationProvider.class);
        }
    }
}
