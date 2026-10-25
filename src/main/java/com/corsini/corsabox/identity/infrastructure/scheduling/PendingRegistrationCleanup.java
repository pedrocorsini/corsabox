package com.corsini.corsabox.identity.infrastructure.scheduling;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.domain.PendingRegistrationRepository;

@Component
public class PendingRegistrationCleanup {
    private static final Logger log = LoggerFactory.getLogger(PendingRegistrationCleanup.class);

    private final PendingRegistrationRepository repository;

    public PendingRegistrationCleanup(PendingRegistrationRepository repository) {
        this.repository = repository;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
    public void purgeExpired() {
        int removed = repository.deleteExpired(Instant.now());
        if (removed > 0) log.info("Cadastros pendentes expirados removidos: {}", removed);
    }
}