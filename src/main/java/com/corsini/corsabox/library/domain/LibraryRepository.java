package com.corsini.corsabox.library.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;

public interface LibraryRepository {
    LibraryEntry save(LibraryEntry entry);
    Optional<LibraryEntry> find(UserId userId, int tmdbId, MediaFormat format);
    Page<LibraryEntry> findByStatus(UserId userId, EntryStatus status, Pageable pageable);
    void deleteById(UUID id);
}