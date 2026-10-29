package com.corsini.corsabox.identity.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import com.corsini.corsabox.identity.domain.exception.EmailAlreadyInUseException;
import com.corsini.corsabox.identity.domain.exception.UsernameAlreadyInUseException;

class UserExceptionHandlerTests {
    private final UserExceptionHandler handler = new UserExceptionHandler();

    @Test void collectionConflictsDoNotClaimAnIdentityIsAlreadyInUse() {
        var response = handler.handleIntegrity(new DataIntegrityViolationException("profile_favorites unique constraint"));
        assertEquals(409, response.getStatus());
        assertFalse(response.getDetail().toLowerCase().contains("email"));
        assertFalse(response.getDetail().toLowerCase().contains("username"));
        assertFalse(response.getDetail().contains("profile_favorites"));
    }

    @Test void identityConflictsKeepTheirSpecificMessages() {
        for (var conflict : new RuntimeException[] {
            new EmailAlreadyInUseException("ana@example.test"), new UsernameAlreadyInUseException("ana")
        }) {
            var response = handler.handleConflict(conflict);
            assertEquals(409, response.getStatus());
            assertEquals(conflict.getMessage(), response.getDetail());
        }
    }
}
