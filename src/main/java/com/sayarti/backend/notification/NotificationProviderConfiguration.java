package com.sayarti.backend.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class NotificationProviderConfiguration {
    @Bean
    @ConditionalOnMissingBean(NotificationProvider.class)
    NotificationProvider unavailableNotificationProvider() {
        return new UnavailableNotificationProvider();
    }
}
