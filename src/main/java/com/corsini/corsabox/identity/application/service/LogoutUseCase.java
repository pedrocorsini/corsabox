package com.corsini.corsabox.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.port.ResetTokenService;
import com.corsini.corsabox.identity.domain.RefreshTokenRepository;

@Service
public class LogoutUseCase {
    private final RefreshTokenRepository refreshTokens;
    private final ResetTokenService tokenService;

    public LogoutUseCase(RefreshTokenRepository refreshTokens, ResetTokenService tokenService) {
        this.refreshTokens = refreshTokens;
        this.tokenService = tokenService;
    }

    @Transactional
    public void execute(String rawToken) {
        refreshTokens.deleteByHash(tokenService.hash(rawToken));
    }
}