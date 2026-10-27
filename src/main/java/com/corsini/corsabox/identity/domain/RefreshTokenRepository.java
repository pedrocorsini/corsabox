package com.corsini.corsabox.identity.domain;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {
    void save(RefreshToken token);

    // Marca como usado de forma atômica. Só uma chamada concorrente consegue.
    Optional<UserId> consume(String tokenHash, Instant now);

    // Dono do token se ele já foi usado antes do instante informado (sinal de reuso).
    Optional<UserId> findReusedUser(String tokenHash, Instant usedBefore);

    void deleteAllFor(UserId userId);

    void deleteByHash(String tokenHash);

    int deleteExpired(Instant now);
}