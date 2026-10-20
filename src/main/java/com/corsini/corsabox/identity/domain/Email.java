package com.corsini.corsabox.identity.domain;

public record Email(String email) {
    // Verifies if the email syntax is valid.
    public Email {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Unvalid email!");
        email = email.toLowerCase();
    }
}
