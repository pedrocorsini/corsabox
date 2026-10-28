package com.corsini.corsabox.identity.application.dto;

public record GoogleIdentity(String subject, String email, String firstName, String lastName) {
    public GoogleIdentity {
        if (subject == null || subject.isBlank() || subject.length() > 255 || email == null || email.isBlank())
            throw new IllegalArgumentException("Invalid Google identity");
        email = email.strip().toLowerCase(java.util.Locale.ROOT);
        firstName = firstName == null ? "" : firstName;
        lastName = lastName == null ? "" : lastName;
    }
}
