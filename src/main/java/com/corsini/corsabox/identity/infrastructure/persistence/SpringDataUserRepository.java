package com.corsini.corsabox.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.corsini.corsabox.identity.infrastructure.entity.UserEntity;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID>{
    Optional<UserEntity> findByEmail(String email);
    boolean existsByUsername(String username);
}
