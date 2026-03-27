package com.example.holdem.user.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String passwordHash;

    @Embedded
    private UserProfile profile;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    protected User() {
    }

    public User(String username, String passwordHash, UserProfile profile, UserStatus status) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.profile = profile;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public UserStatus getStatus() {
        return status;
    }
}
