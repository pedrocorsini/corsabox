package com.corsini.corsabox.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update RefreshTokenEntity t set t.usedAt = :now
        where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now
        """)
    int markUsed(@Param("hash") String hash, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshTokenEntity t where t.userId = :userId")
    int removeAllByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshTokenEntity t where t.tokenHash = :hash")
    int removeByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}