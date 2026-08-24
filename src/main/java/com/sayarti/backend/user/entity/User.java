package com.sayarti.backend.user.entity;

import com.sayarti.backend.common.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "users")
public class User extends BaseAuditableEntity {
    @Id
    private UUID id;
    @Nationalized
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Nationalized
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(name = "password_hash", length = 100)
    private String passwordHash;
    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;
    @Column(name = "country_code", length = 2)
    private String countryCode;
    @Column(name = "default_currency_code", length = 3)
    private String defaultCurrencyCode;
    @Column(name = "preferred_language", nullable = false, length = 2)
    private String preferredLanguage = "en";
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
    }
    public User(String firstName, String lastName, String email, String passwordHash) {
        this.id = UUID.randomUUID();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authProvider = AuthProvider.LOCAL;
        this.emailVerified = false;
    }

    public User(String firstName, String lastName, String email, String passwordHash,
            String countryCode, String defaultCurrencyCode) {
        this(firstName, lastName, email, passwordHash);
        this.countryCode = countryCode;
        this.defaultCurrencyCode = defaultCurrencyCode;
    }

    public static User google(
            String firstName, String lastName, String email, String googleSubject) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.firstName = firstName;
        user.lastName = lastName;
        user.email = email;
        user.googleSubject = googleSubject;
        user.authProvider = AuthProvider.GOOGLE;
        user.emailVerified = true;
        return user;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isEmailVerified() { return emailVerified; }
    public String getCountryCode() { return countryCode; }
    public String getDefaultCurrencyCode() { return defaultCurrencyCode; }
    public String getPreferredLanguage() { return preferredLanguage == null ? "en" : preferredLanguage; }
    public boolean isCountrySetupComplete() {
        return countryCode != null && defaultCurrencyCode != null;
    }

    public void selectCountry(String countryCode, String defaultCurrencyCode) {
        this.countryCode = countryCode;
        this.defaultCurrencyCode = defaultCurrencyCode;
    }

    public void changeDefaultCurrency(String currencyCode) {
        this.defaultCurrencyCode = currencyCode;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void updateProfile(String firstName, String lastName) {
        if (firstName != null) {
            this.firstName = firstName.trim();
        }
        if (lastName != null) {
            this.lastName = lastName.trim();
        }
    }

    public void changePreferredLanguage(String language) {
        if (language != null) this.preferredLanguage = language.toLowerCase(java.util.Locale.ROOT);
    }

    public void delete() {
        this.deletedAt = Instant.now();
    }
}
