package com.corsini.corsabox.library.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import lombok.Getter;

@Getter
public class EpisodeLog {
    private final UUID id;
    private final UUID entryId;
    private final int seasonNumber;
    private final int episodeNumber;
    private final Instant createdAt;
    private Rating rating;
    private LocalDate watchedOn;
    private String review;
    private Instant updatedAt;
    private Instant publishedAt;
    private boolean commentsEnabled = true;

    public void setPublication(boolean publish, boolean commentsEnabled) {
        if (publish && review == null)
            throw new IllegalArgumentException("Write a review before publishing it");
        this.publishedAt = publish ? (publishedAt == null ? Instant.now() : publishedAt) : null;
        this.commentsEnabled = commentsEnabled;
    }

    public void restorePublication(Instant publishedAt, boolean commentsEnabled) {
        this.publishedAt = publishedAt;
        this.commentsEnabled = commentsEnabled;
    }

    private EpisodeLog(UUID id, UUID entryId, int seasonNumber, int episodeNumber,
                       Rating rating, LocalDate watchedOn, String review,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.entryId = entryId;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.rating = rating;
        this.watchedOn = watchedOn;
        this.review = review;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EpisodeLog create(UUID entryId, int season, int episode,
                                    Rating rating, LocalDate watchedOn, String review) {
        if (season < 0 || episode < 1) throw new IllegalArgumentException("Invalid episode");
        var now = Instant.now();
        var log = new EpisodeLog(UUID.randomUUID(), entryId, season, episode, null, null, null, now, now);
        log.update(rating, watchedOn, review);
        return log;
    }

    public static EpisodeLog restore(UUID id, UUID entryId, int season, int episode, Rating rating,
                                     LocalDate watchedOn, String review, Instant createdAt, Instant updatedAt) {
        return new EpisodeLog(id, entryId, season, episode, rating, watchedOn, review, createdAt, updatedAt);
    }

    public void update(Rating rating, LocalDate watchedOn, String review) {
        if (watchedOn != null && watchedOn.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1)))
            throw new IllegalArgumentException("Watched date cannot be in the future");
        var text = review == null || review.isBlank() ? null : review.strip();
        if (text != null && text.length() > LibraryEntry.MAX_REVIEW_LENGTH)
            throw new IllegalArgumentException("Review must have at most " + LibraryEntry.MAX_REVIEW_LENGTH + " characters");
        this.rating = rating;
        this.watchedOn = watchedOn;
        this.review = text;
        if (text == null) this.publishedAt = null;
        this.updatedAt = Instant.now();
    }
}
