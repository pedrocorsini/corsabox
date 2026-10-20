package com.corsini.corsabox.identity.application.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserRepository;

@Service 
public class ListUserUseCase {
    private final UserRepository userRepository;

    public ListUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<User> execute(Pageable pageable){
        return userRepository.findAll(pageable);
    }   

}
