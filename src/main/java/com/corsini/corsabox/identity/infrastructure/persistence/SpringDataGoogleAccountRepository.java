package com.corsini.corsabox.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGoogleAccountRepository extends JpaRepository<GoogleAccountEntity, String> {}
