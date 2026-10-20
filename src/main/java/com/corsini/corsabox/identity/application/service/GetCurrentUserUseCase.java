package com.corsini.corsabox.identity.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.InvalidCredentialsException;

@Service
public class GetCurrentUserUseCase {
    private final UserRepository userRepository;

    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User execute(UUID id) {
        return userRepository.findById(new UserId(id))
                .orElseThrow(InvalidCredentialsException::new);
    }
}