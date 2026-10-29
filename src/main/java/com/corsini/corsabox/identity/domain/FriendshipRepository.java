package com.corsini.corsabox.identity.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.corsini.corsabox.identity.domain.Friendship.Relationship;
import com.corsini.corsabox.identity.domain.Friendship.RequestDirection;

public interface FriendshipRepository {
    boolean lockUsers(UUID first, UUID second);
    Optional<Friendship> find(UUID first, UUID second);
    Friendship save(Friendship friendship);
    void delete(UUID id);
    Map<UUID, Relationship> relationships(UUID viewerId, Collection<UUID> userIds);
    Page<User> friends(UUID viewerId, Pageable pageable);
    Page<User> requests(UUID viewerId, RequestDirection direction, Pageable pageable);
}
