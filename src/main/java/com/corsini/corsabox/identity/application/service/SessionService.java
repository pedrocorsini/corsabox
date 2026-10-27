package com.corsini.corsabox.identity.application.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.corsini.corsabox.identity.application.dto.AuthTokens;
import com.corsini.corsabox.identity.application.port.ResetTokenService;
import com.corsini.corsabox.identity.application.port.TokenIssuer;
import com.corsini.corsabox.identity.domain.RefreshToken;
import com.corsini.corsabox.identity.domain.RefreshTokenRepository;
import com.corsini.corsabox.identity.domain.User;

@Service
public class SessionService {
    private final TokenIssuer tokenIssuer;
    private final ResetTokenService tokenService;
    private final RefreshTokenRepository refreshTokens;
    private final Duration refreshTtl;

    public SessionService(TokenIssuer tokenIssuer, ResetTokenService tokenService,
                          RefreshTokenRepository refreshTokens,
                          @Value("${app.jwt.refresh-ttl-days:30}") long refreshTtlDays) {
        this.tokenIssuer = tokenIssuer;
        this.tokenService = tokenService;
        this.refreshTokens = refreshTokens;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    /** Emite o access token e um refresh token novo. Só o hash do refresh vai para o banco. */
    public AuthTokens open(User user) {
        var raw = tokenService.generate();
        refreshTokens.save(RefreshToken.issue(user.getId(), tokenService.hash(raw), refreshTtl));
        return new AuthTokens(tokenIssuer.issue(user), raw, refreshTtl);
    }
}