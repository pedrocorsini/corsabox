package com.corsini.corsabox.identity.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.corsini.corsabox.identity.domain.*;

@Repository
public class GoogleAccountJpaRepository implements GoogleAccountRepository {
    private final SpringDataGoogleAccountRepository accounts;
    private final SpringDataUserRepository users;

    public GoogleAccountJpaRepository(SpringDataGoogleAccountRepository accounts, SpringDataUserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    public Optional<UserId> findUserId(String subject) {
        return accounts.findById(subject).map(a -> new UserId(a.getUser().getId()));
    }

    public void link(String subject, UserId userId) {
        // Persist rather than merge: an existing subject must never be reassigned.
        var account = new GoogleAccountEntity();
        account.setSubject(subject);
        account.setUser(users.getReferenceById(userId.id()));
        entityManager.persist(account);
        entityManager.flush();
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
}
