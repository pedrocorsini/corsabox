package com.corsini.corsabox.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.corsini.corsabox.identity.application.port.ResetTokenService;
import com.corsini.corsabox.identity.domain.*;
import com.corsini.corsabox.identity.domain.exception.*;
import com.corsini.corsabox.identity.infrastructure.security.CaffeineLoginAttemptPolicy;

class SecurityBehaviorTests {
    @Test
    void targetedAccountIsBlockedBeforePasswordVerificationAfterFiveFailures() {
        var users = mock(UserRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var sessions = mock(SessionService.class);
        var policy = new CaffeineLoginAttemptPolicy();
        when(encoder.encode(anyString())).thenReturn("dummy-hash");
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        var login = new LoginUseCase(users, encoder, sessions, policy);
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class,
                    () -> login.execute("target@example.test", "wrong"));
        }
        clearInvocations(encoder, users);
        assertThrows(TooManyLoginAttemptsException.class,
                () -> login.execute("target@example.test", "correct-password"));
        verifyNoInteractions(encoder, users, sessions);
        assertDoesNotThrow(() -> policy.checkAllowed("other@example.test"));
    }

    @Test
    void replayInsideGraceDoesNotRevokeSuccessor() {
        assertReplayRevocation(1, false);
    }

    @Test
    void replayOutsideGraceRevokesSessions() {
        assertReplayRevocation(20, true);
    }

    private void assertReplayRevocation(long secondsAgo, boolean revoked) {
        var tokens = mock(RefreshTokenRepository.class);
        var users = mock(UserRepository.class);
        var hashes = mock(ResetTokenService.class);
        var sessions = mock(SessionService.class);
        var userId = new UserId();
        var usedAt = Instant.now().minusSeconds(secondsAgo);
        when(hashes.hash("dummy-token")).thenReturn("hash");
        when(tokens.consume(eq("hash"), any())).thenReturn(Optional.empty());
        when(tokens.findReusedUser(eq("hash"), any())).thenAnswer(call ->
                usedAt.isBefore(call.getArgument(1, Instant.class))
                        ? Optional.of(userId) : Optional.empty());
        var refresh = new RefreshSessionUseCase(tokens, users, hashes, sessions);
        assertThrows(InvalidRefreshTokenException.class, () -> refresh.execute("dummy-token"));
        verify(tokens, times(revoked ? 1 : 0)).deleteAllFor(userId);
        verifyNoInteractions(users, sessions);
    }
}
