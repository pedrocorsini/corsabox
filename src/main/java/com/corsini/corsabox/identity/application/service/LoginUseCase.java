package com.corsini.corsabox.identity.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.port.LoginAttemptPolicy;
import com.corsini.corsabox.identity.application.port.TokenIssuer;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.InvalidCredentialsException;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final LoginAttemptPolicy attempts;
    // Hash descartável com o mesmo custo do encoder, usado quando o email não existe.
    private final String dummyHash;

    public LoginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        TokenIssuer tokenIssuer, LoginAttemptPolicy attempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.attempts = attempts;
        this.dummyHash = passwordEncoder.encode("senha-descartavel");
    }

    @Transactional(readOnly = true)
    public String execute(String rawEmail, String password) {
        var email = rawEmail.strip().toLowerCase();
        attempts.checkAllowed(email);

        var user = userRepository.findByEmail(email).filter(u -> u.getPasswordHash() != null);
        var hash = user.map(User::getPasswordHash).orElse(dummyHash);
        var matches = passwordEncoder.matches(password, hash);

        if (user.isEmpty() || !matches) {
            attempts.recordFailure(email);
            throw new InvalidCredentialsException();
        }

        attempts.reset(email);
        return tokenIssuer.issue(user.get());
    }
}