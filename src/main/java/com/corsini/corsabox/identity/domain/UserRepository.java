package com.corsini.corsabox.identity.domain;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Page<User> findAll(Pageable pageable);
    Page<User> search(String query, UserId viewerId, Pageable pageable);
}
