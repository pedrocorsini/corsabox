package com.corsini.corsabox.identity.domain;

public record Username(String username) {
    // Verifies if the username syntax is valid.
    public Username {
        if (username == null || !username.matches("^[a-z0-9_]{3,20}$"))
            throw new IllegalArgumentException("Username inválido");
    }
}
