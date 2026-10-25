package com.corsini.corsabox.identity.api.response;

import java.time.Instant;
import java.util.UUID;

import com.corsini.corsabox.identity.domain.User;

public record UserSummaryResponse(UUID id, String username, String firstName, String lastName, Instant createdAt) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
            user.getId().id(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getCreatedAt());
    }
}