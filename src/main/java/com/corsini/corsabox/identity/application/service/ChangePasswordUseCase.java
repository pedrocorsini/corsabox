package com.corsini.corsabox.identity.application.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.domain.RefreshTokenRepository;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.UserNotFoundException;
import com.corsini.corsabox.identity.domain.exception.WrongCurrentPasswordException;

@Service
public class ChangePasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokens;

    public ChangePasswordUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokens) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public void execute(UUID userId, String currentPassword, String newPassword) {
        var user = userRepository.findById(new UserId(userId))
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new WrongCurrentPasswordException();

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokens.deleteAllFor(new UserId(userId));
    }
}