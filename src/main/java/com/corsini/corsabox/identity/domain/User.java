package com.corsini.corsabox.identity.domain;

import java.time.Instant;

import lombok.Getter;

@Getter 
public class User {

    // User Attributes
    private final UserId id;
    private final Email email;
    private String firstName;
    private String lastName;
    private Username username;
    private final Instant createdAt;

    // User Constructor 
    public User(UserId id, Email email, Username username, String firstName, String lastName, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
    }

    public static User register(Email email, Username username, String firstName, String lastName) {
        return new User(
            new UserId(), 
            email, 
            username, 
            requireText(firstName, "First Name"), 
            requireText(lastName, "Last Name"), 
            Instant.now());
    }

    public static User restore(UserId id, Email email, Username username, String firstName, String lastName, Instant createdAt) {
        return new User(id, email, username, firstName, lastName, createdAt);
    }

    public void changeUsername(Username newUsername) {
        this.username = newUsername;
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
}
    

