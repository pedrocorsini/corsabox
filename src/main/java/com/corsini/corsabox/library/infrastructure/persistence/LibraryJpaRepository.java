package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.library.domain.EntryStatus;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.MediaSnapshot;
import com.corsini.corsabox.library.domain.Rating;

@Repository
public class LibraryJpaRepository implements LibraryRepository {

    private final SpringDataLibraryEntryRepository repo;

    public LibraryJpaRepository(SpringDataLibraryEntryRepository repo) {
        this.repo = repo;
    }

    private static LibraryEntryEntity toEntity(LibraryEntry e) {
        var m = e.getMedia();
        var entity = new LibraryEntryEntity();
        entity.setId(e.getId());
        entity.setUserId(e.getUserId().id());
        entity.setTmdbId(m.tmdbId());
        entity.setFormat(m.format());
        entity.setKind(m.kind());
        entity.setTitle(m.title());
        entity.setReleaseYear(m.year());
        entity.setPosterUrl(m.posterUrl());
        entity.setBackdropUrl(m.backdropUrl());
        entity.setStatus(e.getStatus());
        entity.setRatingHalves(e.getRating() == null ? null : e.getRating().halves());
        entity.setWatchedOn(e.getWatchedOn());
        entity.setReview(e.getReview());
        entity.setCreatedAt(e.getCreatedAt());
        entity.setUpdatedAt(e.getUpdatedAt());
        return entity;
    }

    private static LibraryEntry toDomain(LibraryEntryEntity e) {
        var media = new MediaSnapshot(e.getTmdbId(), e.getFormat(), e.getKind(), e.getTitle(),
                e.getReleaseYear(), e.getPosterUrl(), e.getBackdropUrl());
        return LibraryEntry.restore(e.getId(), new UserId(e.getUserId()), media, e.getStatus(),
                e.getRatingHalves() == null ? null : new Rating(e.getRatingHalves()),
                e.getWatchedOn(), e.getReview(), e.getCreatedAt(), e.getUpdatedAt());
    }

    @Override
    public LibraryEntry save(LibraryEntry entry) {
        repo.save(toEntity(entry));
        return entry;
    }

    @Override
    public Optional<LibraryEntry> find(UserId userId, int tmdbId, MediaFormat format) {
        return repo.findByUserIdAndTmdbIdAndFormat(userId.id(), tmdbId, format).map(LibraryJpaRepository::toDomain);
    }

    @Override
    public Page<LibraryEntry> findByStatus(UserId userId, EntryStatus status, Pageable pageable) {
        // Assistidos: data mais recente primeiro, sem data por último. Watchlist: adicionados por último primeiro.
        var sort = status == EntryStatus.WATCHED
                ? Sort.by(Sort.Order.desc("watchedOn").nullsLast(), Sort.Order.desc("createdAt"))
                : Sort.by(Sort.Order.desc("createdAt"));
        var sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return repo.findByUserIdAndStatus(userId.id(), status, sorted).map(LibraryJpaRepository::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }
}