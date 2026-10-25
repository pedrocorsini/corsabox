package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.corsini.corsabox.library.domain.EpisodeLog;
import com.corsini.corsabox.library.domain.EpisodeLogRepository;
import com.corsini.corsabox.library.domain.Rating;

@Repository
public class EpisodeLogJpaRepository implements EpisodeLogRepository {
    private final SpringDataEpisodeLogRepository repo;

    public EpisodeLogJpaRepository(SpringDataEpisodeLogRepository repo) { this.repo = repo; }

    private static EpisodeLogEntity toEntity(EpisodeLog l) {
        var e = new EpisodeLogEntity();
        e.setId(l.getId());
        e.setEntryId(l.getEntryId());
        e.setSeasonNumber(l.getSeasonNumber());
        e.setEpisodeNumber(l.getEpisodeNumber());
        e.setRatingHalves(l.getRating() == null ? null : l.getRating().halves());
        e.setWatchedOn(l.getWatchedOn());
        e.setReview(l.getReview());
        e.setCreatedAt(l.getCreatedAt());
        e.setUpdatedAt(l.getUpdatedAt());
        return e;
    }

    private static EpisodeLog toDomain(EpisodeLogEntity e) {
        return EpisodeLog.restore(e.getId(), e.getEntryId(), e.getSeasonNumber(), e.getEpisodeNumber(),
                e.getRatingHalves() == null ? null : new Rating(e.getRatingHalves()),
                e.getWatchedOn(), e.getReview(), e.getCreatedAt(), e.getUpdatedAt());
    }

    @Override public EpisodeLog save(EpisodeLog log) { repo.save(toEntity(log)); return log; }

    @Override public void saveAll(List<EpisodeLog> logs) {
        repo.saveAll(logs.stream().map(EpisodeLogJpaRepository::toEntity).toList());
    }

    @Override public Optional<EpisodeLog> find(UUID entryId, int season, int episode) {
        return repo.findByEntryIdAndSeasonNumberAndEpisodeNumber(entryId, season, episode)
                .map(EpisodeLogJpaRepository::toDomain);
    }

    @Override public List<EpisodeLog> findByEntry(UUID entryId) {
        return repo.findByEntryIdOrderBySeasonNumberAscEpisodeNumberAsc(entryId).stream()
                .map(EpisodeLogJpaRepository::toDomain).toList();
    }

    @Override public void delete(UUID id) { repo.deleteById(id); }
    @Override public void deleteByEntry(UUID entryId) { repo.deleteByEntryId(entryId); }
}