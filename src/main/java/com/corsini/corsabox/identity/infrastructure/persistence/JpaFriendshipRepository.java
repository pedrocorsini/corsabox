package com.corsini.corsabox.identity.infrastructure.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.corsini.corsabox.identity.domain.Friendship;
import com.corsini.corsabox.identity.domain.Friendship.Relationship;
import com.corsini.corsabox.identity.domain.Friendship.RequestDirection;
import com.corsini.corsabox.identity.domain.Friendship.Status;
import com.corsini.corsabox.identity.domain.FriendshipRepository;
import com.corsini.corsabox.identity.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Repository
public class JpaFriendshipRepository implements FriendshipRepository {
    private final EntityManager em;
    public JpaFriendshipRepository(EntityManager em) { this.em = em; }

    @Override
    public boolean lockUsers(UUID first, UUID second) {
        // Lock in one global order, including when the relationship does not exist yet.
        return em.createQuery("select u from UserEntity u where u.id in :ids order by u.id", UserEntity.class)
                .setParameter("ids", List.of(first, second)).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList().size() == 2;
    }

    @Override
    public Optional<Friendship> find(UUID first, UUID second) {
        return em.createQuery("""
                select f from FriendshipEntity f where f.firstUser.id = :first and f.secondUser.id = :second
                """, FriendshipEntity.class).setParameter("first", first).setParameter("second", second)
                .getResultStream().findFirst().map(this::toDomain);
    }

    @Override
    public Friendship save(Friendship friendship) {
        var entity = new FriendshipEntity();
        entity.setId(friendship.id());
        entity.setFirstUser(em.getReference(UserEntity.class, friendship.firstUserId()));
        entity.setSecondUser(em.getReference(UserEntity.class, friendship.secondUserId()));
        entity.setRequestedBy(friendship.requestedBy());
        entity.setStatus(friendship.status());
        entity.setCreatedAt(friendship.createdAt());
        em.merge(entity);
        return friendship;
    }

    @Override
    public void delete(UUID id) { em.remove(em.getReference(FriendshipEntity.class, id)); }

    @Override
    public Map<UUID, Relationship> relationships(UUID viewerId, Collection<UUID> userIds) {
        var result = new HashMap<UUID, Relationship>();
        if (userIds.isEmpty()) return result;
        var rows = em.createQuery("""
                select f from FriendshipEntity f where
                (f.firstUser.id = :viewer and f.secondUser.id in :ids)
                or (f.secondUser.id = :viewer and f.firstUser.id in :ids)
                """, FriendshipEntity.class).setParameter("viewer", viewerId).setParameter("ids", userIds).getResultList();
        rows.forEach(entity -> {
            var friendship = toDomain(entity);
            var other = friendship.firstUserId().equals(viewerId) ? friendship.secondUserId() : friendship.firstUserId();
            result.put(other, friendship.relationship(viewerId));
        });
        if (userIds.contains(viewerId)) result.put(viewerId, Relationship.SELF);
        return result;
    }

    @Override
    public Page<User> friends(UUID viewerId, Pageable pageable) {
        return people(viewerId, Status.ACCEPTED, "", pageable);
    }

    @Override
    public Page<User> requests(UUID viewerId, RequestDirection direction, Pageable pageable) {
        return people(viewerId, Status.PENDING,
                direction == RequestDirection.RECEIVED ? " and f.requestedBy = u.id" : " and f.requestedBy = :viewer", pageable);
    }

    private Page<User> people(UUID viewerId, Status status, String direction, Pageable pageable) {
        var where = """
                from UserEntity u where exists (select f.id from FriendshipEntity f where f.status = :status
                and ((f.firstUser.id = :viewer and f.secondUser.id = u.id)
                or (f.secondUser.id = :viewer and f.firstUser.id = u.id))
                """ + direction + ")";
        var rows = em.createQuery("select u " + where + " order by u.username, u.id", UserEntity.class)
                .setParameter("viewer", viewerId).setParameter("status", status)
                .setFirstResult(Math.toIntExact(pageable.getOffset())).setMaxResults(pageable.getPageSize())
                .getResultList().stream().map(UserJpaRepository::toDomain).toList();
        var total = em.createQuery("select count(u) " + where, Long.class)
                .setParameter("viewer", viewerId).setParameter("status", status).getSingleResult();
        return new PageImpl<>(rows, pageable, total);
    }

    private Friendship toDomain(FriendshipEntity f) {
        return new Friendship(f.getId(), f.getFirstUser().getId(), f.getSecondUser().getId(),
                f.getRequestedBy(), f.getStatus(), f.getCreatedAt());
    }
}
