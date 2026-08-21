package com.sayarti.backend.user.entity;

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
public class User {
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
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
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
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
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
        user.createdAt = Instant.now();
        user.updatedAt = user.createdAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isEmailVerified() { return emailVerified; }

    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String firstName, String lastName) {
        if (firstName != null) {
            this.firstName = firstName.trim();
        }
        if (lastName != null) {
            this.lastName = lastName.trim();
        }
        this.updatedAt = Instant.now();
    }

    public void delete() {
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
