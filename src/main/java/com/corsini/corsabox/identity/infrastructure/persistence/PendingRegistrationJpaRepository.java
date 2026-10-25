package com.corsini.corsabox.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.domain.PendingRegistration;
import com.corsini.corsabox.identity.domain.PendingRegistrationRepository;

@Repository
public class PendingRegistrationJpaRepository implements PendingRegistrationRepository {
    private final SpringDataPendingRegistrationRepository repo;

    public PendingRegistrationJpaRepository(SpringDataPendingRegistrationRepository repo) { this.repo = repo; }

    @Override
    public void save(PendingRegistration p) {
        var e = new PendingRegistrationEntity();
        e.setEmail(p.email());
        e.setUsername(p.username());
        e.setFirstName(p.firstName());
        e.setLastName(p.lastName());
        e.setPasswordHash(p.passwordHash());
        e.setCodeHash(p.codeHash());
        e.setExpiresAt(p.expiresAt());
        e.setAttempts(p.attempts());
        e.setLastSentAt(p.lastSentAt());
        repo.save(e);
    }

    @Override
    public Optional<PendingRegistration> findByEmail(String email) {
        return repo.findById(email).map(e -> new PendingRegistration(e.getEmail(), e.getUsername(),
                e.getFirstName(), e.getLastName(), e.getPasswordHash(), e.getCodeHash(),
                e.getExpiresAt(), e.getAttempts(), e.getLastSentAt()));
    }

    @Override
    public void deleteByEmail(String email) { repo.deleteById(email); }

    @Override
    @Transactional
    public int deleteExpired(Instant now) { return repo.deleteExpired(now); }
}