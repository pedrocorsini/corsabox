package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataEpisodeLogRepository extends JpaRepository<EpisodeLogEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EpisodeLogEntity> findByEntryIdAndSeasonNumberAndEpisodeNumber(UUID entryId, int season, int episode);

    List<EpisodeLogEntity> findByEntryIdOrderBySeasonNumberAscEpisodeNumberAsc(UUID entryId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EpisodeLogEntity e where e.entryId = :entryId")
    int deleteByEntryId(@Param("entryId") UUID entryId);

    @Query(value = """
            select distinct on (entry_id) entry_id, season_number, episode_number
            from episode_logs
            where entry_id in (:ids)
            order by entry_id, (season_number > 0) desc, season_number desc, episode_number desc
            """, nativeQuery = true)
    List<Object[]> findLatestByEntryIds(@Param("ids") Collection<UUID> ids);
}
