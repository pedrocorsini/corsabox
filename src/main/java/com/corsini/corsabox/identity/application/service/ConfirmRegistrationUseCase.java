package com.corsini.corsabox.identity.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.port.VerificationCodeService;
import com.corsini.corsabox.identity.domain.PendingRegistrationRepository;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.EmailAlreadyInUseException;
import com.corsini.corsabox.identity.domain.exception.InvalidVerificationCodeException;
import com.corsini.corsabox.identity.domain.exception.UsernameAlreadyInUseException;

@Service
public class ConfirmRegistrationUseCase {
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final VerificationCodeService codeService;

    public ConfirmRegistrationUseCase(UserRepository userRepository, PendingRegistrationRepository pendingRepository,
                                      VerificationCodeService codeService) {
        this.userRepository = userRepository;
        this.pendingRepository = pendingRepository;
        this.codeService = codeService;
    }

    // noRollbackFor: sem isso o contador de tentativas e o delete seriam desfeitos junto com a exceção.
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public User execute(String rawEmail, String code) {
        var email = rawEmail.strip().toLowerCase();
        var pending = pendingRepository.findByEmail(email).orElseThrow(InvalidVerificationCodeException::new);

        if (pending.expired(Instant.now()) || pending.attempts() >= MAX_ATTEMPTS) {
            pendingRepository.deleteByEmail(email);
            throw new InvalidVerificationCodeException();
        }

        if (!codeService.matches(email, code, pending.codeHash())) {
            pendingRepository.save(pending.withAttempts(pending.attempts() + 1));
            throw new InvalidVerificationCodeException();
        }

        // Alguém pode ter ocupado o email/username entre o passo 1 e o 2.
        if (userRepository.findByEmail(email).isPresent()) throw new EmailAlreadyInUseException(email);
        if (userRepository.existsByUsername(pending.username())) throw new UsernameAlreadyInUseException(pending.username());

        var user = userRepository.save(User.register(
                pending.email(), pending.username(), pending.firstName(), pending.lastName(), pending.passwordHash()));
        pendingRepository.deleteByEmail(email);
        return user;
    }
}