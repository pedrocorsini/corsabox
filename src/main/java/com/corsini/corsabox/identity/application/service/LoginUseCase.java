package com.corsini.corsabox.identity.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.port.TokenIssuer;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.InvalidCredentialsException;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    
    public LoginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    // Login
    @Transactional(readOnly = true)
    public String execute(String email, String password) {
        var user = userRepository.findByEmail(email.strip().toLowerCase())
                .filter(u -> u.getPasswordHash() != null
                        && passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return tokenIssuer.issue(user);
    }
}