package com.corsini.corsabox.identity.application.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.dto.AuthTokens;
import com.corsini.corsabox.identity.application.port.ResetTokenService;
import com.corsini.corsabox.identity.domain.RefreshTokenRepository;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.InvalidRefreshTokenException;

@Service
public class RefreshSessionUseCase {
    private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    private final RefreshTokenRepository refreshTokens;
    private final UserRepository userRepository;
    private final ResetTokenService tokenService;
    private final SessionService sessions;

    public RefreshSessionUseCase(RefreshTokenRepository refreshTokens, UserRepository userRepository,
                                 ResetTokenService tokenService, SessionService sessions) {
        this.refreshTokens = refreshTokens;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.sessions = sessions;
    }

    // noRollbackFor: sem isso a revogação por reuso seria desfeita junto com a exceção.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokens execute(String rawToken) {
        var hash = tokenService.hash(rawToken);
        var now = Instant.now();

        var userId = refreshTokens.consume(hash, now).orElse(null);
        if (userId == null) {
            refreshTokens.findReusedUser(hash, now.minus(REUSE_GRACE))
                    .ifPresent(refreshTokens::deleteAllFor);
            throw new InvalidRefreshTokenException();
        }

        var user = userRepository.findById(userId).orElseThrow(InvalidRefreshTokenException::new);
        return sessions.open(user);
    }
}