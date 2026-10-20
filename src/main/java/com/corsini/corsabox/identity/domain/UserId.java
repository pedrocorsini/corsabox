package com.corsini.corsabox.identity.domain;

import java.util.UUID;

import org.springframework.util.Assert;

public record UserId(UUID id) {
    public UserId{
        Assert.notNull(id, "id must not be null.");
    }

    // Creates random UUID
    public UserId(){
        this(UUID.randomUUID());
    }
}
