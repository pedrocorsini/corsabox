package com.corsini.corsabox.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class RefreshToken {
    private final UUID id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;

    private RefreshToken(UUID id, UserId userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static RefreshToken issue(UserId userId, String tokenHash, Duration ttl) {
        var now = Instant.now();
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, now.plus(ttl), now);
    }

    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}