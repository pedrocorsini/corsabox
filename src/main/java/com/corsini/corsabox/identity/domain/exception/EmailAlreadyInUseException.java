package com.corsini.corsabox.identity.domain.exception;

public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException(String email) {
        super("Email already used: " + email);
    }
}

