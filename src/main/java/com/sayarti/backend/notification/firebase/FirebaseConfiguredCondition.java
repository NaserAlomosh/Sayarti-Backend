package com.sayarti.backend.notification.firebase;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

final class FirebaseConfiguredCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty("sayarti.firebase.project-id"))
                && StringUtils.hasText(context.getEnvironment().getProperty(
                        "sayarti.firebase.client-email"))
                && StringUtils.hasText(context.getEnvironment().getProperty(
                        "sayarti.firebase.private-key"));
    }
}
