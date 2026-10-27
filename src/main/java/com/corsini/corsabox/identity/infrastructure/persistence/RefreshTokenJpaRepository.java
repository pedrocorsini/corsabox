package com.corsini.corsabox.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.domain.RefreshToken;
import com.corsini.corsabox.identity.domain.RefreshTokenRepository;
import com.corsini.corsabox.identity.domain.UserId;

@Repository
public class RefreshTokenJpaRepository implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository repo;

    public RefreshTokenJpaRepository(SpringDataRefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(RefreshToken token) {
        var e = new RefreshTokenEntity();
        e.setId(token.getId());
        e.setUserId(token.getUserId().id());
        e.setTokenHash(token.getTokenHash());
        e.setExpiresAt(token.getExpiresAt());
        e.setCreatedAt(token.getCreatedAt());
        repo.save(e);
    }

    @Override
    public Optional<UserId> consume(String tokenHash, Instant now) {
        if (repo.markUsed(tokenHash, now) == 0) return Optional.empty();
        return repo.findByTokenHash(tokenHash).map(e -> new UserId(e.getUserId()));
    }

    @Override
    public Optional<UserId> findReusedUser(String tokenHash, Instant usedBefore) {
        return repo.findByTokenHash(tokenHash)
                .filter(e -> e.getUsedAt() != null && e.getUsedAt().isBefore(usedBefore))
                .map(e -> new UserId(e.getUserId()));
    }

    @Override
    public void deleteAllFor(UserId userId) {
        repo.removeAllByUserId(userId.id());
    }

    @Override
    public void deleteByHash(String tokenHash) {
        repo.removeByTokenHash(tokenHash);
    }

    @Override
    @Transactional
    public int deleteExpired(Instant now) {
        return repo.deleteExpired(now);
    }
}