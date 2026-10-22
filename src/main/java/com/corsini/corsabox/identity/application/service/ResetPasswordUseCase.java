package com.corsini.corsabox.identity.application.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.port.ResetTokenService;
import com.corsini.corsabox.identity.domain.PasswordResetTokenRepository;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.InvalidResetTokenException;
import com.corsini.corsabox.identity.domain.exception.UserNotFoundException;

@Service
public class ResetPasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResetTokenService tokenService;

    public ResetPasswordUseCase(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                ResetTokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public void execute(String rawToken, String newPassword) {
        var userId = tokenRepository.consume(tokenService.hash(rawToken), Instant.now())
                .orElseThrow(InvalidResetTokenException::new);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.id()));

        if (user.getPasswordHash() == null)
            throw new InvalidResetTokenException();

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}   