package com.corsini.corsabox.identity.application.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.corsini.corsabox.identity.domain.Friendship;
import com.corsini.corsabox.identity.domain.Friendship.Relationship;
import com.corsini.corsabox.identity.domain.Friendship.RequestDirection;
import com.corsini.corsabox.identity.domain.FriendshipRepository;
import com.corsini.corsabox.identity.domain.User;
import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.FriendshipException;
import com.corsini.corsabox.identity.domain.exception.UserNotFoundException;
import static com.corsini.corsabox.identity.domain.exception.FriendshipException.Reason.*;

@Service
@Transactional
public class PeopleService {
    private final UserRepository users;
    private final FriendshipRepository friendships;

    public PeopleService(UserRepository users, FriendshipRepository friendships) {
        this.users = users;
        this.friendships = friendships;
    }

    public record Person(User user, Relationship relationship) {}

    @Transactional(readOnly = true)
    public Person profileByUsername(UUID viewer, String username) {
        var user = users.findByUsername(username).orElseThrow(() -> new UserNotFoundException("Perfil não encontrado."));
        return profile(viewer, user.getId().id());
    }

    @Transactional(readOnly = true)
    public Person profile(UUID viewer, UUID target) {
        var user = users.findById(new UserId(target)).orElseThrow(() -> new UserNotFoundException(target));
        var relationship = friendships.relationships(viewer, List.of(target)).getOrDefault(target, Relationship.NONE);
        return new Person(user, relationship);
    }

    @Transactional(readOnly = true)
    public Page<Person> search(UUID viewer, String query, int page, int size) {
        var normalized = query.strip().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("@")) normalized = normalized.substring(1);
        if (normalized.length() > 100) throw new IllegalArgumentException("A busca deve ter até 100 caracteres.");
        var pageable = pageable(page, size);
        if (normalized.length() < 2) return Page.empty(pageable);
        return withRelationships(viewer, users.search(normalized, new UserId(viewer), pageable));
    }

    @Transactional(readOnly = true)
    public Page<Person> friends(UUID viewer, int page, int size) {
        return withRelationships(viewer, friendships.friends(viewer, pageable(page, size)));
    }

    @Transactional(readOnly = true)
    public Page<Person> requests(UUID viewer, RequestDirection direction, int page, int size) {
        return withRelationships(viewer, friendships.requests(viewer, direction, pageable(page, size)));
    }

    public Relationship request(UUID viewer, UUID target) {
        lock(viewer, target);
        var existing = friendships.find(Friendship.first(viewer, target), Friendship.second(viewer, target));
        if (existing.isPresent()) {
            var relationship = existing.get().relationship(viewer);
            if (relationship == Relationship.REQUEST_RECEIVED) throw new FriendshipException(INCOMING_REQUEST);
            return relationship;
        }
        return friendships.save(Friendship.request(viewer, target)).relationship(viewer);
    }

    public Relationship accept(UUID viewer, UUID target) {
        lock(viewer, target);
        var existing = friendships.find(Friendship.first(viewer, target), Friendship.second(viewer, target))
                .orElseThrow(() -> new FriendshipException(NOT_FOUND));
        return friendships.save(existing.accept(viewer)).relationship(viewer);
    }

    public Relationship remove(UUID viewer, UUID target) {
        lock(viewer, target);
        friendships.find(Friendship.first(viewer, target), Friendship.second(viewer, target))
                .ifPresent(friendship -> friendships.delete(friendship.id()));
        return Relationship.NONE;
    }

    private void lock(UUID viewer, UUID target) {
        if (viewer.equals(target)) throw new FriendshipException(SELF_REQUEST);
        if (!friendships.lockUsers(Friendship.first(viewer, target), Friendship.second(viewer, target)))
            throw new UserNotFoundException(target);
    }

    private Page<Person> withRelationships(UUID viewer, Page<User> page) {
        var relationships = friendships.relationships(viewer, page.stream().map(user -> user.getId().id()).toList());
        return page.map(user -> new Person(user, relationships.getOrDefault(user.getId().id(), Relationship.NONE)));
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
    }
}
