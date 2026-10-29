package com.corsini.corsabox.library.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;

public interface LibraryRepository {
    java.util.List<LibraryEntry> findWatchedByIds(UUID userId, java.util.List<UUID> ids);
    Page<LibraryEntry> searchWatched(UUID userId, com.corsini.corsabox.catalog.domain.MediaKind kind, String pattern, Pageable pageable);
    LibraryEntry save(LibraryEntry entry);
    Optional<LibraryEntry> find(UserId userId, int tmdbId, MediaFormat format);
    Page<LibraryEntry> findByStatus(UserId userId, EntryStatus status, Pageable pageable);
    Page<PublishedReview> findPublished(Pageable pageable);
    Page<PublishedReview> findPublishedByAuthor(UUID authorId, Pageable pageable);
    void deleteById(UUID id);
    void updateMedia(UUID entryId, MediaSnapshot media);
}
