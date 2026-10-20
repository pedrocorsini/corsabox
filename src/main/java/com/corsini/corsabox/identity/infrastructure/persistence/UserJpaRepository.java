package com.corsini.corsabox.identity.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.corsini.corsabox.identity.domain.Email;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.Username;
import com.corsini.corsabox.identity.infrastructure.entity.UserEntity;

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
        entity.setUsername(user.getUsername().username());
        entity.setEmail(user.getEmail().email());
        entity.setCreatedOn(user.getCreatedAt());
        
        return entity;
    }

    private static User toDomain(UserEntity entity){
        return new User(
            new UserId(entity.getId()), 
            new Email(entity.getEmail()), 
            new Username(entity.getUsername()), 
            entity.getFirstName(), 
            entity.getLastName(), 
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
    public Optional<User> findByEmail(Email email) {
        return springDataUserRepository.findByEmail(email.email()).map(UserJpaRepository::toDomain);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return springDataUserRepository.existsByUsername(username.username());
    }
    
}
