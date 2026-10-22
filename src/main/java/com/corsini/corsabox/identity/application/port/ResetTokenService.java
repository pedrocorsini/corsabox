package com.corsini.corsabox.identity.application.port;

public interface ResetTokenService {
    /** Gera um token aleatório em texto puro (vai só no email). */
    String generate();

    /** Hash determinístico do token, que é o que fica no banco. */
    String hash(String rawToken);
}