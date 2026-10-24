package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.library.domain.EntryStatus;

public interface SpringDataLibraryEntryRepository extends JpaRepository<LibraryEntryEntity, UUID> {
    Optional<LibraryEntryEntity> findByUserIdAndTmdbIdAndFormat(UUID userId, int tmdbId, MediaFormat format);
    Page<LibraryEntryEntity> findByUserIdAndStatus(UUID userId, EntryStatus status, Pageable pageable);
}