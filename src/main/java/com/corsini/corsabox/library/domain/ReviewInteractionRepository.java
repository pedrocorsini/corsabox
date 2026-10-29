package com.corsini.corsabox.library.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewInteractionRepository {
    record ReviewAccess(boolean published, boolean commentsEnabled) {}
    record Activity(ReactionSummary reactions, long commentCount) {
        public static final Activity EMPTY = new Activity(ReactionSummary.EMPTY, 0);
    }

    Optional<ReviewAccess> findReview(UUID id, boolean lock);
    Page<ReviewComment> comments(UUID reviewId, Pageable pageable);
    Optional<ReviewComment> findComment(UUID reviewId, UUID commentId);
    ReviewComment saveComment(ReviewComment comment);
    void deleteComment(UUID commentId);
    void setReaction(UUID reviewId, UUID commentId, UUID userId, Reaction reaction);
    Map<UUID, Activity> activities(Collection<UUID> reviewIds, UUID viewerId);
    Map<UUID, ReactionSummary> commentReactions(Collection<UUID> commentIds, UUID viewerId);
}
