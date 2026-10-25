package com.corsini.corsabox.identity.domain;

import java.time.Instant;
import java.util.Optional;

public interface PendingRegistrationRepository {
    void save(PendingRegistration pending);          // upsert por email
    Optional<PendingRegistration> findByEmail(String email);
    void deleteByEmail(String email);
    int deleteExpired(Instant now); 
}