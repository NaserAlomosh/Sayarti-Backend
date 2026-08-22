package com.sayarti.backend.notification;

public final class NotificationProviderException extends Exception {
    private final boolean permanentlyInvalidToken;

    private NotificationProviderException(String message, Throwable cause,
            boolean permanentlyInvalidToken) {
        super(message, cause);
        this.permanentlyInvalidToken = permanentlyInvalidToken;
    }

    public static NotificationProviderException permanentlyInvalidToken(Throwable cause) {
        return new NotificationProviderException("Registration token is no longer usable", cause, true);
    }

    public static NotificationProviderException temporaryOrUnknown(Throwable cause) {
        return new NotificationProviderException("Notification provider delivery failed", cause, false);
    }

    public boolean isPermanentlyInvalidToken() {
        return permanentlyInvalidToken;
    }
}
