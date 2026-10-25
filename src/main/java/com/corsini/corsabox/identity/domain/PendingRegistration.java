package com.corsini.corsabox.identity.domain;

import java.time.Instant;

public record PendingRegistration(String email, String username, String firstName, String lastName,
        String passwordHash, String codeHash, Instant expiresAt, int attempts, Instant lastSentAt) {

    public boolean expired(Instant now) { return !expiresAt.isAfter(now); }

    public PendingRegistration withAttempts(int attempts) {
        return new PendingRegistration(email, username, firstName, lastName, passwordHash,
                codeHash, expiresAt, attempts, lastSentAt);
    }
}