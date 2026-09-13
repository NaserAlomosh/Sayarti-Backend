package com.sayarti.backend.notification.firebase;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
final class FirebaseConfiguredCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (!context.getEnvironment().getProperty(
                "sayarti.firebase.enabled", Boolean.class, true)) {
            return false;
        }
        String path = context.getEnvironment().getProperty(
                "sayarti.firebase.service-account-path");
        try {
            FirebaseConfiguration.loadCredentials(path);
            return true;
        } catch (Exception exception) {
            // Invalid or inaccessible credentials leave Firebase disabled so the existing
            // unavailable notification provider can fail delivery safely.
            return false;
        }
    }
}
