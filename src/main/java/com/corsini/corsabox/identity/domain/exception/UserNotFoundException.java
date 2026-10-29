package com.corsini.corsabox.identity.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) { super(message); }

    public UserNotFoundException(UUID id) {
        super("User not found: " + id);
    }

}
