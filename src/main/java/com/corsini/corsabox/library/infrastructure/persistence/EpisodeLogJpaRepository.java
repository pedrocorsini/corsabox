package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;

import com.corsini.corsabox.library.domain.EpisodeLog;
import com.corsini.corsabox.library.domain.EpisodeLogRepository;
import com.corsini.corsabox.library.domain.EpisodeProgress;
import com.corsini.corsabox.library.domain.Rating;

@Repository
public class EpisodeLogJpaRepository implements EpisodeLogRepository {
    private final SpringDataEpisodeLogRepository repo;
    private final EntityManager em;

    public EpisodeLogJpaRepository(SpringDataEpisodeLogRepository repo, EntityManager em) {
        this.repo = repo;
        this.em = em;
    }

    private EpisodeLogEntity toEntity(EpisodeLog l) {
        var e = new EpisodeLogEntity();
        e.setId(l.getId());
        e.setEntryId(l.getEntryId());
        e.setEntry(em.getReference(LibraryEntryEntity.class, l.getEntryId()));
        e.setSeasonNumber(l.getSeasonNumber());
        e.setEpisodeNumber(l.getEpisodeNumber());
        e.setRatingHalves(l.getRating() == null ? null : l.getRating().halves());
        e.setWatchedOn(l.getWatchedOn());
        e.setReview(l.getReview());
        e.setPublishedAt(l.getPublishedAt());
        e.setCommentsEnabled(l.isCommentsEnabled());
        e.setCreatedAt(l.getCreatedAt());
        e.setUpdatedAt(l.getUpdatedAt());
        return e;
    }

    static EpisodeLog toDomain(EpisodeLogEntity e) {
        var log = EpisodeLog.restore(e.getId(), e.getEntryId(), e.getSeasonNumber(), e.getEpisodeNumber(),
                e.getRatingHalves() == null ? null : new Rating(e.getRatingHalves()),
                e.getWatchedOn(), e.getReview(), e.getCreatedAt(), e.getUpdatedAt());
        log.restorePublication(e.getPublishedAt(), e.isCommentsEnabled());
        return log;
    }

    @Override
    public EpisodeLog save(EpisodeLog log) {
        repo.save(toEntity(log));
        return log;
    }

    @Override
    public void saveAll(List<EpisodeLog> logs) {
        repo.saveAll(logs.stream().map(this::toEntity).toList());
    }

    @Override
    public Optional<EpisodeLog> find(UUID entryId, int season, int episode) {
        return repo.findByEntryIdAndSeasonNumberAndEpisodeNumber(entryId, season, episode)
                .map(EpisodeLogJpaRepository::toDomain);
    }

    @Override
    public List<EpisodeLog> findByEntry(UUID entryId) {
        return repo.findByEntryIdOrderBySeasonNumberAscEpisodeNumberAsc(entryId).stream()
                .map(EpisodeLogJpaRepository::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        repo.deleteById(id);
    }

    @Override
    public void deleteByEntry(UUID entryId) {
        repo.deleteByEntryId(entryId);
    }

    @Override
    public Map<UUID, EpisodeProgress> latestByEntry(Collection<UUID> entryIds) {
        if (entryIds.isEmpty())
            return Map.of();
        var result = new HashMap<UUID, EpisodeProgress>();
        for (var row : repo.findLatestByEntryIds(entryIds)) {
            result.put((UUID) row[0],
                    new EpisodeProgress(((Number) row[1]).intValue(), ((Number) row[2]).intValue()));
        }
        return result;
    }
}
