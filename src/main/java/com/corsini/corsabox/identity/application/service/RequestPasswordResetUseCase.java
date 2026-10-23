package com.corsini.corsabox.identity.application.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.port.PasswordResetMailer;
import com.corsini.corsabox.identity.application.port.ResetTokenService;
import com.corsini.corsabox.identity.domain.PasswordResetToken;
import com.corsini.corsabox.identity.domain.PasswordResetTokenRepository;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.EmailNotFoundException;

@Service
    public class RequestPasswordResetUseCase {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetMailer mailer;
    private final ResetTokenService tokenService;
    private final Duration ttl;

    public RequestPasswordResetUseCase(UserRepository userRepository,
                                    PasswordResetTokenRepository tokenRepository,
                                    PasswordResetMailer mailer,
                                    ResetTokenService tokenService,
                                    @Value("${app.password-reset.ttl-minutes:30}") long ttlMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailer = mailer;
        this.tokenService = tokenService;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }
    @Transactional
    public void execute(String email) {
        var user = userRepository.findByEmail(email.strip().toLowerCase())
                .filter(u -> u.getPasswordHash() != null)
                .orElseThrow(EmailNotFoundException::new);

        tokenRepository.invalidateAllFor(user.getId(), Instant.now());

        var raw = tokenService.generate();
        tokenRepository.save(PasswordResetToken.issue(user.getId(), tokenService.hash(raw), ttl));

        mailer.send(user, raw);
    }
}   