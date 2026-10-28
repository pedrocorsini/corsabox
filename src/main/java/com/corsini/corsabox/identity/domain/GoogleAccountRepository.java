package com.corsini.corsabox.identity.domain;

import java.util.Optional;

public interface GoogleAccountRepository {
    Optional<UserId> findUserId(String subject);
    void link(String subject, UserId userId);
}
