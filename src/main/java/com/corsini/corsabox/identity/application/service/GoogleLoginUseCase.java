package com.corsini.corsabox.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.corsini.corsabox.identity.application.dto.*;
import com.corsini.corsabox.identity.domain.*;
import com.corsini.corsabox.identity.domain.exception.*;

@Service
public class GoogleLoginUseCase {
    private final GoogleAccountRepository accounts;
    private final UserRepository users;
    private final LoginUseCase login;
    private final SessionService sessions;

    public GoogleLoginUseCase(GoogleAccountRepository accounts, UserRepository users,
                              LoginUseCase login, SessionService sessions) {
        this.accounts = accounts;
        this.users = users;
        this.login = login;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public String nextStep(GoogleIdentity identity) {
        if (accounts.findUserId(identity.subject()).isPresent()) return "login";
        return users.findByEmail(identity.email()).isPresent() ? "link" : "register";
    }

    @Transactional
    public AuthTokens complete(GoogleIdentity identity, String username, String firstName,
                               String lastName, String password) {
        var linked = accounts.findUserId(identity.subject());
        if (linked.isPresent()) {
            return sessions.open(users.findById(linked.get())
                    .orElseThrow(() -> new UserNotFoundException(linked.get().id())));
        }
        var existing = users.findByEmail(identity.email());
        if (existing.isPresent()) {
            if (password == null || password.isBlank()) throw new InvalidCredentialsException();
            // Reuse password verification and account lockout. All writes share this transaction.
            var tokens = login.execute(identity.email(), password);
            accounts.link(identity.subject(), existing.get().getId());
            return tokens;
        }
        var user = User.register(identity.email(), username, firstName, lastName, null);
        if (users.existsByUsername(user.getUsername())) throw new UsernameAlreadyInUseException(user.getUsername());
        user = users.save(user);
        accounts.link(identity.subject(), user.getId());
        return sessions.open(user);
    }
}
