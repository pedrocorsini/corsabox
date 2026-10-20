package com.corsini.corsabox.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.corsini.corsabox.identity.application.dto.RegisterUserCommand;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.EmailAlreadyInUseException;
import com.corsini.corsabox.identity.domain.exception.UsernameAlreadyInUseException;



@Service 
public class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional 
    public User execute(RegisterUserCommand command){
        var user = User.register(command.email(), command.username(), command.firstName(), command.lastName(), passwordEncoder.encode(command.password()));

        if (userRepository.findByEmail(user.getEmail()).isPresent()) 
            throw new EmailAlreadyInUseException(user.getEmail());

        if (userRepository.existsByUsername(user.getUsername())) 
            throw new UsernameAlreadyInUseException(user.getUsername());
        
        return userRepository.save(user);
    }
}
