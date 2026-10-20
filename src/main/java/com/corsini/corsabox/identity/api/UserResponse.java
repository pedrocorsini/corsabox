package com.corsini.corsabox.identity.api;

import java.time.Instant;
import java.util.UUID;

import com.corsini.corsabox.identity.domain.User;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record UserResponse(UUID id, String email, String firstName, String lastName, String username, Instant createdAt) {
    public static UserResponse from(User user){
        return new UserResponse(
            user.getId().id(), 
            user.getEmail(), 
            user.getFirstName(),
            user.getLastName(),
            user.getUsername(),
            user.getCreatedAt()
        );
    }
}
