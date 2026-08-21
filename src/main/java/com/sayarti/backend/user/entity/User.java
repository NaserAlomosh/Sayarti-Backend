package com.sayarti.backend.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "users")
public class User {
    @Id private UUID id;
    @Nationalized @Column(name="first_name", nullable=false, length=100) private String firstName;
    @Nationalized @Column(name="last_name", nullable=false, length=100) private String lastName;
    @Column(nullable=false, unique=true, length=320) private String email;
    @Column(name="password_hash", length=100) private String passwordHash;
    @Column(name="google_subject", unique=true, length=255) private String googleSubject;
    @Enumerated(EnumType.STRING) @Column(name="auth_provider", nullable=false, length=20) private AuthProvider authProvider;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Column(name="deleted_at") private Instant deletedAt;

    protected User() {}
    public User(String firstName, String lastName, String email, String passwordHash) {
        this.id=UUID.randomUUID(); this.firstName=firstName; this.lastName=lastName; this.email=email;
        this.passwordHash=passwordHash; this.authProvider=AuthProvider.LOCAL;
        this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
    public static User google(String firstName, String lastName, String email, String googleSubject) {
        User user = new User();
        user.id = UUID.randomUUID(); user.firstName = firstName; user.lastName = lastName; user.email = email;
        user.googleSubject = googleSubject; user.authProvider = AuthProvider.GOOGLE;
        user.createdAt = Instant.now(); user.updatedAt = user.createdAt;
        return user;
    }
    public UUID getId(){return id;} public String getFirstName(){return firstName;} public String getLastName(){return lastName;}
    public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;}
    public AuthProvider getAuthProvider(){return authProvider;} public Instant getCreatedAt(){return createdAt;}
    public String getGoogleSubject(){return googleSubject;}
}
