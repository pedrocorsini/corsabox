package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.Objects;
import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.identity.infrastructure.persistence.UserEntity;
import com.corsini.corsabox.library.domain.EntryStatus;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.corsini.corsabox.library.domain.LibraryRepository;
import com.corsini.corsabox.library.domain.MediaSnapshot;
import com.corsini.corsabox.library.domain.Rating;
import com.corsini.corsabox.library.domain.PublishedReview;

@Repository
public class LibraryJpaRepository implements LibraryRepository {

    private final SpringDataLibraryEntryRepository repo;
    private final EntityManager em;

    public LibraryJpaRepository(SpringDataLibraryEntryRepository repo, EntityManager em) {
        this.repo = repo;
        this.em = em;
    }

    @Override
    public List<LibraryEntry> findWatchedByIds(UUID userId, List<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return repo.findByUserIdAndStatusAndIdIn(userId, EntryStatus.WATCHED, ids).stream()
                .map(LibraryJpaRepository::toDomain).toList();
    }

    @Override
    public Page<LibraryEntry> searchWatched(UUID userId, com.corsini.corsabox.catalog.domain.MediaKind kind, String pattern, Pageable pageable) {
        return repo.searchWatched(userId, kind, pattern, pageable).map(LibraryJpaRepository::toDomain);
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
        entity.setPublishedAt(e.getPublishedAt());
        entity.setCommentsEnabled(e.isCommentsEnabled());
        entity.setCreatedAt(e.getCreatedAt());
        entity.setUpdatedAt(e.getUpdatedAt());
        entity.setReleaseDate(m.releaseDate());
        entity.setRuntimeMinutes(m.runtimeMinutes());
        entity.setGenres(joinGenres(m.genres()));
        return entity;
    }

    private static LibraryEntry toDomain(LibraryEntryEntity e) {
        var media = new MediaSnapshot(e.getTmdbId(), e.getFormat(), e.getKind(), e.getTitle(),
            e.getReleaseYear(), e.getReleaseDate(), e.getRuntimeMinutes(), splitGenres(e.getGenres()),
            e.getPosterUrl(), e.getBackdropUrl());
        var entry = LibraryEntry.restore(e.getId(), new UserId(e.getUserId()), media, e.getStatus(),
                e.getRatingHalves() == null ? null : new Rating(e.getRatingHalves()),
                e.getWatchedOn(), e.getReview(), e.getCreatedAt(), e.getUpdatedAt());
        entry.restorePublication(e.getPublishedAt(), e.isCommentsEnabled());
        return entry;
    }
    
    private static String joinGenres(List<String> genres) {
        return String.join("|", genres);
    }

    private static List<String> splitGenres(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.split("\\|"));
    }
    
    @Override
    public void updateMedia(UUID entryId, MediaSnapshot media) {
        repo.updateMetadata(entryId, media.releaseDate(), media.runtimeMinutes(), joinGenres(media.genres()));
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
    public Page<PublishedReview> findPublished(Pageable pageable) {
        return findPublishedByAuthor(null, pageable);
    }

    @Override
    public Page<PublishedReview> findPublishedByAuthor(UUID authorId, Pageable pageable) {
        // Paginate the combined timeline in the database, then hydrate only this page.
        var authorFilter = authorId == null ? "" : " and e.user_id = :author";
        var timeline = """
                select e.id, e.published_at from library_entries e join users u on u.id = e.user_id
                where e.published_at is not null and e.review is not null and e.status = 'WATCHED'
                """ + authorFilter + " union all " + """
                select l.id, l.published_at from episode_logs l
                join library_entries e on e.id = l.entry_id join users u on u.id = e.user_id
                where l.published_at is not null and l.review is not null
                """ + authorFilter;
        var pageQuery = em.createNativeQuery("select id from (" + timeline + ") feed order by published_at desc, id desc", UUID.class);
        var countQuery = em.createNativeQuery("select count(*) from (" + timeline + ") feed", Long.class);
        if (authorId != null) {
            pageQuery.setParameter("author", authorId);
            countQuery.setParameter("author", authorId);
        }
        @SuppressWarnings("unchecked")
        List<UUID> ids = pageQuery.setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize()).getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();
        if (ids.isEmpty()) return new PageImpl<>(List.of(), pageable, total);
        var reviews = new HashMap<UUID, PublishedReview>();
        em.createQuery("""
                select e, u from LibraryEntryEntity e join UserEntity u on u.id = e.userId where e.id in :ids
                and e.publishedAt is not null and e.review is not null and e.status = 'WATCHED'
                """, Object[].class)
                .setParameter("ids", ids).getResultList().forEach(row -> {
                    var e = (LibraryEntryEntity) row[0];
                    var u = (UserEntity) row[1];
                    reviews.put(e.getId(), new PublishedReview(toDomain(e), u.getId(), u.getUsername(), u.getFirstName(), u.getLastName()));
                });
        em.createQuery("""
                select l, e, u from EpisodeLogEntity l join l.entry e join UserEntity u on u.id = e.userId
                where l.id in :ids and l.publishedAt is not null and l.review is not null
                """, Object[].class).setParameter("ids", ids).getResultList().forEach(row -> {
                    var l = (EpisodeLogEntity) row[0];
                    var e = (LibraryEntryEntity) row[1];
                    var u = (UserEntity) row[2];
                    reviews.put(l.getId(), new PublishedReview(toDomain(e), u.getId(), u.getUsername(), u.getFirstName(),
                            u.getLastName(), EpisodeLogJpaRepository.toDomain(l)));
                });
        return new PageImpl<>(ids.stream().map(reviews::get).filter(Objects::nonNull).toList(), pageable, total);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }
}
