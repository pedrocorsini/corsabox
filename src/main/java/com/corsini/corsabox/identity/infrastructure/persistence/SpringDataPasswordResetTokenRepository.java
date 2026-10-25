package com.corsini.corsabox.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update PasswordResetTokenEntity t set t.usedAt = :now
        where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now
        """)
    int markUsed(@Param("hash") String hash, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update PasswordResetTokenEntity t set t.usedAt = :now
        where t.userId = :userId and t.usedAt is null
        """)
    int invalidateOpenTokens(@Param("userId") UUID userId, @Param("now") Instant now);
    Optional<PasswordResetTokenEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}