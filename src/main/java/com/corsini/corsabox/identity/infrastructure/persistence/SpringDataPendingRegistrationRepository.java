package com.corsini.corsabox.identity.infrastructure.persistence;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataPendingRegistrationRepository extends JpaRepository<PendingRegistrationEntity, String> {

    @Modifying
    @Query("delete from PendingRegistrationEntity p where p.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}