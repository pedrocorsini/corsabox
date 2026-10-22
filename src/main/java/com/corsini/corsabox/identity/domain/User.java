package com.corsini.corsabox.identity.domain;

import java.time.Instant;
import java.util.regex.Pattern;

import lombok.Getter;

@Getter 
public class User {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,20}$");
    
    private final UserId id;
    private final String email;
    private String firstName;
    private String lastName;
    private String username;
    private final Instant createdAt;
    private String passwordHash;

    // Private constructor 
    private User(UserId id, String email, String username, String firstName, String lastName, String passwordHash, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    // Register 
    public static User register(String email, String username, String firstName, String lastName, String passwordHash) {
        return new User(
            new UserId(), 
            requireText(email, "Email").toLowerCase(), 
            normalizeUsername(username), 
            requireText(firstName, "First Name"), 
            requireText(lastName, "Last Name"), 
            passwordHash,
            Instant.now());
    }

    public static User restore(UserId id, String email, String username, String firstName, String lastName, String passwordHash, Instant createdAt) {
        return new User(id, email, username, firstName, lastName, passwordHash, createdAt);
    }

    public void changeUsername(String newUsername) {
        this.username = normalizeUsername(newUsername);
    }

    public void changeName(String firstName, String lastName) {
        this.firstName = requireText(firstName, "First Name");
        this.lastName = requireText(lastName, "Last Name");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " it is mandatory!");
        return value.strip();
    }

    private static String normalizeUsername(String value) {
        String username = requireText(value, "Username").toLowerCase();
        if (!USERNAME.matcher(username).matches())
            throw new IllegalArgumentException("Invalid username");
    return username;
    }

    public void changePassword(String newPasswordHash) {
    if (newPasswordHash == null || newPasswordHash.isBlank())
        throw new IllegalArgumentException("Password hash is mandatory!");
    this.passwordHash = newPasswordHash;
    }
}
    

