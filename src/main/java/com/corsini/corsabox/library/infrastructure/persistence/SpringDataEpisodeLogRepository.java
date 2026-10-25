package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataEpisodeLogRepository extends JpaRepository<EpisodeLogEntity, UUID> {
    Optional<EpisodeLogEntity> findByEntryIdAndSeasonNumberAndEpisodeNumber(UUID entryId, int season, int episode);
    List<EpisodeLogEntity> findByEntryIdOrderBySeasonNumberAscEpisodeNumberAsc(UUID entryId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EpisodeLogEntity e where e.entryId = :entryId")
    int deleteByEntryId(@Param("entryId") UUID entryId);
}