package com.corsini.corsabox.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.corsini.corsabox.identity.domain.*;
import com.corsini.corsabox.identity.domain.exception.UserNotFoundException;

@Repository
public class JpaPublicProfileRepository implements PublicProfileRepository {
    private final EntityManager em;
    public JpaPublicProfileRepository(EntityManager em) { this.em = em; }
    public Optional<PublicProfile> find(UUID id) {
        var e = em.find(PublicProfileEntity.class, id);
        return e == null ? Optional.empty() : Optional.of(new PublicProfile(id, e.getBio(), e.getPhoto(), e.getFavorites(),
            e.getCatalogFavorites().stream().map(FavoriteMedia::toDomain).toList()));
    }
    public void lockUser(UUID id) {
        if (em.find(UserEntity.class, id, LockModeType.PESSIMISTIC_WRITE) == null) throw new UserNotFoundException(id);
    }
    public void save(PublicProfile p) {
        var e = em.find(PublicProfileEntity.class, p.userId());
        if (e == null) { e = new PublicProfileEntity(); e.setUserId(p.userId()); em.persist(e); }
        e.setBio(p.bio()); e.setPhoto(p.photo());
        var catalog = e.getCatalogFavorites().stream().map(FavoriteMedia::toDomain).toList();
        if (!catalog.equals(p.catalogFavorites())) {
            e.getCatalogFavorites().clear();
            em.flush();
            e.getCatalogFavorites().addAll(p.catalogFavorites().stream().map(FavoriteMedia::new).toList());
        }
        if (!e.getFavorites().equals(p.favorites())) {
            // Flush removals before inserting new positions: deployed databases also enforce
            // uniqueness of (user_id, entry_id), so in-place swaps would collide.
            e.getFavorites().clear();
            em.flush();
            e.getFavorites().addAll(p.favorites());
        }
    }
}
