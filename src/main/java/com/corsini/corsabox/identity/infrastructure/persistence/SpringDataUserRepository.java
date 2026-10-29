package com.corsini.corsabox.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID>{
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsernameIgnoreCase(String username);
    boolean existsByUsername(String username);

    @Query("""
        select u from UserEntity u where u.id <> :viewer and
        (lower(u.username) like :pattern escape '!'
        or lower(concat(u.firstName, ' ', u.lastName)) like :pattern escape '!')
        """)
    Page<UserEntity> search(@Param("pattern") String pattern, @Param("viewer") UUID viewer, Pageable pageable);
}
