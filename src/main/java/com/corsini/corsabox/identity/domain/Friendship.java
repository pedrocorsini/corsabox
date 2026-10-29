package com.corsini.corsabox.identity.domain;

import java.time.Instant;
import java.util.UUID;
import com.corsini.corsabox.identity.domain.exception.FriendshipException;
import static com.corsini.corsabox.identity.domain.exception.FriendshipException.Reason.*;

public record Friendship(UUID id, UUID firstUserId, UUID secondUserId, UUID requestedBy, Status status, Instant createdAt) {
    public enum Status { PENDING, ACCEPTED }
    public enum Relationship { SELF, NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIENDS }
    public enum RequestDirection { RECEIVED, SENT }

    public static UUID first(UUID a, UUID b) { return a.toString().compareTo(b.toString()) < 0 ? a : b; }
    public static UUID second(UUID a, UUID b) { return first(a, b).equals(a) ? b : a; }

    public static Friendship request(UUID sender, UUID recipient) {
        if (sender.equals(recipient)) throw new FriendshipException(SELF_REQUEST);
        return new Friendship(UUID.randomUUID(), first(sender, recipient), second(sender, recipient), sender, Status.PENDING, Instant.now());
    }

    public Relationship relationship(UUID viewer) {
        if (!viewer.equals(firstUserId) && !viewer.equals(secondUserId)) throw new FriendshipException(NOT_FOUND);
        return status == Status.ACCEPTED ? Relationship.FRIENDS
                : requestedBy.equals(viewer) ? Relationship.REQUEST_SENT : Relationship.REQUEST_RECEIVED;
    }

    public Friendship accept(UUID viewer) {
        var relation = relationship(viewer);
        if (relation == Relationship.FRIENDS) return this;
        if (relation != Relationship.REQUEST_RECEIVED) throw new FriendshipException(NOT_RECIPIENT);
        return new Friendship(id, firstUserId, secondUserId, requestedBy, Status.ACCEPTED, createdAt);
    }
}
