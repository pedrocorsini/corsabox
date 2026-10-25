package com.corsini.corsabox.identity.domain;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository {
    void save(PasswordResetToken token);

    // Invalida os tokens ainda abertos do usuário (só o link mais recente vale). 
    void invalidateAllFor(UserId userId, Instant now);

    // Marca como usado de forma atômica. Só uma chamada concorrente consegue; as outras recebem vazio.
    Optional<UserId> consume(String tokenHash, Instant now);

    // Quando o último link foi emitido para o usuário, usado ou não.
    Optional<Instant> lastIssuedAt(UserId userId);
}