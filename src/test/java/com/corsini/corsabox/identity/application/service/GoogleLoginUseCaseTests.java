package com.corsini.corsabox.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.corsini.corsabox.identity.application.dto.GoogleIdentity;
import com.corsini.corsabox.identity.domain.*;
import com.corsini.corsabox.identity.domain.exception.*;

class GoogleLoginUseCaseTests {
    private final GoogleAccountRepository accounts = mock(GoogleAccountRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LoginUseCase login = mock(LoginUseCase.class);
    private final SessionService sessions = mock(SessionService.class);
    private final GoogleLoginUseCase google = new GoogleLoginUseCase(accounts, users, login, sessions);
    private final GoogleIdentity identity = new GoogleIdentity("google-sub", "person@example.test", "Ana", "Silva");
    private final User user = User.register(identity.email(), "ana", "Ana", "Silva", "hash");

    @BeforeEach
    void defaults() {
        when(accounts.findUserId(anyString())).thenReturn(Optional.empty());
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void newAccountRequiresUsernameAndCreatesPasswordlessUser() {
        assertEquals("register", google.nextStep(identity));
        assertThrows(IllegalArgumentException.class, () -> google.complete(identity, "", "Ana", "Silva", null));
        verifyNoInteractions(sessions);
        google.complete(identity, "ANA_123", "Ana", "Silva", null);
        verify(users).save(argThat(u -> u.getUsername().equals("ana_123") && u.getPasswordHash() == null));
        verify(accounts).link(eq(identity.subject()), any(UserId.class));
        verify(sessions).open(any(User.class));
        verifyNoInteractions(login);
    }

    @Test
    void sameEmailNeverLinksWithoutPasswordProof() {
        when(users.findByEmail(identity.email())).thenReturn(Optional.of(user));
        assertEquals("link", google.nextStep(identity));
        assertThrows(InvalidCredentialsException.class, () -> google.complete(identity, null, null, null, null));
        when(login.execute(identity.email(), "wrong")).thenThrow(new InvalidCredentialsException());
        assertThrows(InvalidCredentialsException.class, () -> google.complete(identity, null, null, null, "wrong"));
        verify(accounts, never()).link(anyString(), any());
        verifyNoInteractions(sessions);
    }

    @Test
    void successfulPasswordAuthenticationLinksExistingAccount() {
        when(users.findByEmail(identity.email())).thenReturn(Optional.of(user));
        google.complete(identity, null, null, null, "correct");
        var order = inOrder(login, accounts);
        order.verify(accounts).findUserId(identity.subject());
        order.verify(login).execute(identity.email(), "correct");
        order.verify(accounts).link(identity.subject(), user.getId());
        verify(users, never()).save(any());
    }

    @Test
    void recurringLoginUsesSubjectEvenAfterGoogleEmailChanges() {
        when(accounts.findUserId(identity.subject())).thenReturn(Optional.of(user.getId()));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        assertEquals("login", google.nextStep(identity));
        google.complete(new GoogleIdentity(identity.subject(), "changed@example.test", "", ""), null, null, null, null);
        verify(sessions).open(user);
        verify(users, never()).findByEmail(anyString());
        verifyNoInteractions(login);
    }

    @Test
    void duplicateUsernameDoesNotLinkOrOpenSession() {
        when(users.existsByUsername("ana")).thenReturn(true);
        assertThrows(UsernameAlreadyInUseException.class, () -> google.complete(identity, "ana", "Ana", "Silva", null));
        verify(accounts, never()).link(anyString(), any());
        verifyNoInteractions(sessions);
    }
}
