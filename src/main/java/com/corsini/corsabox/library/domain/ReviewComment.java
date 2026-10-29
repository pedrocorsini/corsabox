package com.corsini.corsabox.library.domain;

import java.time.Instant;
import java.util.UUID;

public record ReviewComment(UUID id, UUID reviewId, UUID authorId, String username, String text, Instant createdAt) {
    public static final int MAX_LENGTH = 2000;

    public ReviewComment {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Escreva um comentário.");
        text = text.strip();
        if (text.length() > MAX_LENGTH) throw new IllegalArgumentException("O comentário deve ter até 2000 caracteres.");
    }
}
