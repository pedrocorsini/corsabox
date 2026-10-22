package com.corsini.corsabox.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.application.dto.UpdateProfileCommand;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.UserNotFoundException;
import com.corsini.corsabox.identity.domain.exception.UsernameAlreadyInUseException;

@Service
public class UpdateProfileUseCase {
    private final UserRepository userRepository;

    public UpdateProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Update username and name
    @Transactional
    public User execute(UpdateProfileCommand command) {
        var user = userRepository.findById(new UserId(command.userId()))
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if (command.firstName() != null || command.lastName() != null) {
            user.changeName(
                command.firstName() != null ? command.firstName() : user.getFirstName(),
                command.lastName() != null ? command.lastName() : user.getLastName());
        }

        if (command.username() != null) {
            var previousUsername = user.getUsername();
            user.changeUsername(command.username());

            if (!user.getUsername().equals(previousUsername)
                    && userRepository.existsByUsername(user.getUsername()))
                throw new UsernameAlreadyInUseException(user.getUsername());
        }

        return userRepository.save(user);
    }
}
