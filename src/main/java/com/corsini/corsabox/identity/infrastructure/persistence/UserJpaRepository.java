package com.corsini.corsabox.identity.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.identity.domain.UserRepository;

@Repository 
public class UserJpaRepository implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    public UserJpaRepository(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    // Mappers 
    private static UserEntity toEntity(User user){
        var entity = new UserEntity();
        entity.setId(user.getId().id());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setCreatedOn(user.getCreatedAt());
        
        return entity;
    }

    private static User toDomain(UserEntity entity){
        return User.restore(
            new UserId(entity.getId()), 
            entity.getEmail(),
            entity.getUsername(), 
            entity.getFirstName(), 
            entity.getLastName(), 
            entity.getPasswordHash(),
            entity.getCreatedOn());
    }

    @Override
    public User save(User user) {
        var entity = toEntity(user);
        springDataUserRepository.save(entity);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springDataUserRepository.findById(id.id()).map(UserJpaRepository::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email.strip().toLowerCase()).map(UserJpaRepository::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username.strip().toLowerCase());
    }

    @SuppressWarnings("null")
    @Override
    public Page<User> findAll(Pageable pageable) {
        var sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(UserEntity::getCreatedOn).descending());
        return springDataUserRepository.findAll(sorted).map(UserJpaRepository::toDomain);
    }
}
