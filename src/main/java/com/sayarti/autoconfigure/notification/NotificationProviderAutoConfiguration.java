package com.sayarti.autoconfigure.notification;

import com.sayarti.backend.notification.NotificationProvider;
import com.sayarti.backend.notification.UnavailableNotificationProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class NotificationProviderAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(NotificationProvider.class)
    NotificationProvider unavailableNotificationProvider() {
        return new UnavailableNotificationProvider();
    }
}
