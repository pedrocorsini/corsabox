package com.corsini.corsabox.library.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.corsini.corsabox.identity.domain.UserId;

import lombok.Getter;

@Getter
public class LibraryEntry {
    public static final int MAX_REVIEW_LENGTH = 5000;

    private final UUID id;
    private final UserId userId;
    private final MediaSnapshot media;
    private final Instant createdAt;
    private EntryStatus status;
    private Rating rating;
    private LocalDate watchedOn;
    private String review;
    private Instant updatedAt;

    private LibraryEntry(UUID id, UserId userId, MediaSnapshot media, EntryStatus status, Rating rating,
                         LocalDate watchedOn, String review, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.media = media;
        this.status = status;
        this.rating = rating;
        this.watchedOn = watchedOn;
        this.review = review;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static LibraryEntry addToWatchlist(UserId userId, MediaSnapshot media) {
        var now = Instant.now();
        return new LibraryEntry(UUID.randomUUID(), userId, media, EntryStatus.WATCHLIST,
                null, null, null, now, now);
    }

    public static LibraryEntry watched(UserId userId, MediaSnapshot media,
                                       Rating rating, LocalDate watchedOn, String review) {
        var entry = addToWatchlist(userId, media);
        entry.markWatched(rating, watchedOn, review);
        return entry;
    }

    public static LibraryEntry restore(UUID id, UserId userId, MediaSnapshot media, EntryStatus status,
                                       Rating rating, LocalDate watchedOn, String review,
                                       Instant createdAt, Instant updatedAt) {
        return new LibraryEntry(id, userId, media, status, rating, watchedOn, review, createdAt, updatedAt);
    }

    /** Serve para marcar como assistido (vindo da fila ou não) e para corrigir os dados depois. */
    public void markWatched(Rating rating, LocalDate watchedOn, String review) {
        // Um dia de folga para fusos à frente do UTC
        if (watchedOn != null && watchedOn.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1)))
            throw new IllegalArgumentException("Watched date cannot be in the future");

        var text = review == null || review.isBlank() ? null : review.strip();
        if (text != null && text.length() > MAX_REVIEW_LENGTH)
            throw new IllegalArgumentException("Review must have at most " + MAX_REVIEW_LENGTH + " characters");

        this.status = EntryStatus.WATCHED;
        this.rating = rating;
        this.watchedOn = watchedOn;
        this.review = text;
        this.updatedAt = Instant.now();
    }
}