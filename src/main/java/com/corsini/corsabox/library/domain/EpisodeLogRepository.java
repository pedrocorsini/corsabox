package com.corsini.corsabox.library.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EpisodeLogRepository {
    EpisodeLog save(EpisodeLog log);
    void saveAll(List<EpisodeLog> logs);
    Optional<EpisodeLog> find(UUID entryId, int season, int episode);
    List<EpisodeLog> findByEntry(UUID entryId);
    void delete(UUID id);
    void deleteByEntry(UUID entryId);
    Map<UUID, EpisodeProgress> latestByEntry(Collection<UUID> entryIds);
}