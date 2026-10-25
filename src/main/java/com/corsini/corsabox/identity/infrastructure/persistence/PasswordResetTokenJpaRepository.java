package com.corsini.corsabox.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.corsini.corsabox.identity.domain.PasswordResetToken;
import com.corsini.corsabox.identity.domain.PasswordResetTokenRepository;
import com.corsini.corsabox.identity.domain.UserId;

@Repository
public class PasswordResetTokenJpaRepository implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository repo;

    public PasswordResetTokenJpaRepository(SpringDataPasswordResetTokenRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(PasswordResetToken token) {
        var e = new PasswordResetTokenEntity();
        e.setId(token.getId());
        e.setUserId(token.getUserId().id());
        e.setTokenHash(token.getTokenHash());
        e.setExpiresAt(token.getExpiresAt());
        e.setCreatedAt(token.getCreatedAt());
        repo.save(e);
    }

    @Override
    public void invalidateAllFor(UserId userId, Instant now) {
        repo.invalidateOpenTokens(userId.id(), now);
    }

    @Override
    public Optional<UserId> consume(String tokenHash, Instant now) {
        if (repo.markUsed(tokenHash, now) == 0) return Optional.empty();
        return repo.findByTokenHash(tokenHash).map(e -> new UserId(e.getUserId()));
    }

    @Override
    public Optional<Instant> lastIssuedAt(UserId userId) {
        return repo.findFirstByUserIdOrderByCreatedAtDesc(userId.id())
                .map(PasswordResetTokenEntity::getCreatedAt);
    }
}