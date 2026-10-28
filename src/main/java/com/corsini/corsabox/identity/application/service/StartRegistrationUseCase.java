package com.corsini.corsabox.identity.application.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.dto.RegisterUserCommand;
import com.corsini.corsabox.identity.application.port.EmailVerificationMailer;
import com.corsini.corsabox.identity.application.port.VerificationCodeService;
import com.corsini.corsabox.identity.domain.PendingRegistration;
import com.corsini.corsabox.identity.domain.PendingRegistrationRepository;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.UsernameAlreadyInUseException;

@Service
public class StartRegistrationUseCase {
    static final Duration CODE_TTL = Duration.ofMinutes(10);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService codeService;
    private final EmailVerificationMailer mailer;

    public StartRegistrationUseCase(UserRepository userRepository, PendingRegistrationRepository pendingRepository,
                                    PasswordEncoder passwordEncoder, VerificationCodeService codeService,
                                    EmailVerificationMailer mailer) {
        this.userRepository = userRepository;
        this.pendingRepository = pendingRepository;
        this.passwordEncoder = passwordEncoder;
        this.codeService = codeService;
        this.mailer = mailer;
    }

    @Transactional
    public void execute(RegisterUserCommand c) {
        // Reaproveita as validações do domínio (nome, username, email). Não persiste nada.
        var candidate = User.register(c.email(), c.username(), c.firstName(), c.lastName(), "pending");

        if (userRepository.findByEmail(candidate.getEmail()).isPresent())
            return;
        if (userRepository.existsByUsername(candidate.getUsername()))
            throw new UsernameAlreadyInUseException(candidate.getUsername());

        var now = Instant.now();
        var existing = pendingRepository.findByEmail(candidate.getEmail());

        if (existing.filter(p -> p.lastSentAt().plus(RESEND_COOLDOWN).isAfter(now)).isPresent()) return;

        // Reenviar o código não zera as tentativas, senão dá 5 palpites novos a cada 60s.
        int attempts = existing
                .filter(p -> !p.expired(now))
                .map(PendingRegistration::attempts)
                .orElse(0);

        var hash = passwordEncoder.encode(c.password());
        var code = codeService.generate();
        pendingRepository.save(new PendingRegistration(
                candidate.getEmail(), candidate.getUsername(), candidate.getFirstName(), candidate.getLastName(),
                hash, codeService.hash(candidate.getEmail(), code), now.plus(CODE_TTL), attempts, now));

        mailer.send(candidate.getEmail(), candidate.getFirstName(), code);
    }
}