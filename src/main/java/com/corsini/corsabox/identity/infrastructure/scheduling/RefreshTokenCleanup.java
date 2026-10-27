package com.corsini.corsabox.identity.infrastructure.scheduling;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.domain.RefreshTokenRepository;

@Component
public class RefreshTokenCleanup {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenRepository repository;

    public RefreshTokenCleanup(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT6H")
    public void purgeExpired() {
        int removed = repository.deleteExpired(Instant.now());
        if (removed > 0) log.info("Refresh tokens expirados removidos: {}", removed);
    }
}