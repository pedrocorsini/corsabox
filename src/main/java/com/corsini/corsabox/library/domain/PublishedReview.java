package com.corsini.corsabox.library.domain;

import java.util.UUID;

public record PublishedReview(LibraryEntry entry, UUID authorId, String username, String firstName, String lastName,
                              EpisodeLog episode) {
    public PublishedReview(LibraryEntry entry, UUID authorId, String username, String firstName, String lastName) {
        this(entry, authorId, username, firstName, lastName, null);
    }

    public UUID id() { return episode == null ? entry.getId() : episode.getId(); }
}
