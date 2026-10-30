package com.corsini.corsabox.library.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.library.domain.EntryStatus;

public interface SpringDataLibraryEntryRepository extends JpaRepository<LibraryEntryEntity, UUID> {
    java.util.List<LibraryEntryEntity> findByUserIdAndStatusAndIdIn(UUID userId, EntryStatus status, java.util.List<UUID> ids);

    @Query("""
        select e from LibraryEntryEntity e where e.userId = :userId and e.status = 'WATCHED'
        and e.kind = :kind and lower(e.title) like :pattern escape '!'
        order by lower(e.title), e.id
        """)
    Page<LibraryEntryEntity> searchWatched(@Param("userId") UUID userId,
        @Param("kind") com.corsini.corsabox.catalog.domain.MediaKind kind,
        @Param("pattern") String pattern, Pageable pageable);

    Optional<LibraryEntryEntity> findByUserIdAndTmdbIdAndFormat(UUID userId, int tmdbId, MediaFormat format);
    Page<LibraryEntryEntity> findByUserIdAndStatus(UUID userId, EntryStatus status, Pageable pageable);
    
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update LibraryEntryEntity e
        set e.releaseDate = :releaseDate, e.runtimeMinutes = :runtime, e.genres = :genres
        where e.id = :id
        """)
    int updateMetadata(@Param("id") UUID id, @Param("releaseDate") LocalDate releaseDate,
                    @Param("runtime") Integer runtime, @Param("genres") String genres);

}
