package com.corsini.corsabox.identity.domain;

import java.util.Optional;
import java.util.UUID;

public interface PublicProfileRepository {
    Optional<PublicProfile> find(UUID userId);
    void lockUser(UUID userId);
    void save(PublicProfile profile);
}
