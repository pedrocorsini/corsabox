package com.corsini.corsabox.library.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.corsini.corsabox.identity.domain.UserId;
import com.corsini.corsabox.identity.domain.UserRepository;
import com.corsini.corsabox.identity.domain.exception.UserNotFoundException;
import com.corsini.corsabox.library.domain.*;
import com.corsini.corsabox.library.domain.exception.ReviewInteractionException;
import static com.corsini.corsabox.library.domain.exception.ReviewInteractionException.Reason.*;

@Service
@Transactional
public class ReviewInteractionService {
    private final ReviewInteractionRepository repository;
    private final UserRepository users;

    public ReviewInteractionService(ReviewInteractionRepository repository, UserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    public record Comment(ReviewComment comment, ReactionSummary reactions) {}

    @Transactional(readOnly = true)
    public Page<Comment> comments(UUID reviewId, UUID viewerId, int page, int size) {
        requireReview(reviewId, false);
        var comments = repository.comments(reviewId, PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))));
        var reactions = repository.commentReactions(comments.stream().map(ReviewComment::id).toList(), viewerId);
        return comments.map(comment -> new Comment(comment, reactions.getOrDefault(comment.id(), ReactionSummary.EMPTY)));
    }

    public Comment addComment(UUID reviewId, UUID userId, String text) {
        var review = requireReview(reviewId, true);
        if (!review.commentsEnabled()) throw new ReviewInteractionException(COMMENTS_CLOSED);
        var user = users.findById(new UserId(userId)).orElseThrow(() -> new UserNotFoundException(userId));
        var comment = new ReviewComment(UUID.randomUUID(), reviewId, userId, user.getUsername(), text, Instant.now());
        return new Comment(repository.saveComment(comment), ReactionSummary.EMPTY);
    }

    public void deleteComment(UUID reviewId, UUID commentId, UUID userId) {
        requireReview(reviewId, true);
        var comment = requireComment(reviewId, commentId);
        if (!comment.authorId().equals(userId)) throw new ReviewInteractionException(NOT_OWNER);
        repository.deleteComment(commentId);
    }

    public ReactionSummary react(UUID reviewId, UUID commentId, UUID userId, Reaction reaction) {
        // All writes for a review share the parent row lock, including comment deletion.
        // The unique constraints remain the final protection against duplicate votes.
        requireReview(reviewId, true);
        if (commentId != null) requireComment(reviewId, commentId);
        users.findById(new UserId(userId)).orElseThrow(() -> new UserNotFoundException(userId));
        repository.setReaction(reviewId, commentId, userId, reaction);
        if (commentId != null)
            return repository.commentReactions(List.of(commentId), userId).getOrDefault(commentId, ReactionSummary.EMPTY);
        return repository.activities(List.of(reviewId), userId)
                .getOrDefault(reviewId, ReviewInteractionRepository.Activity.EMPTY).reactions();
    }

    private ReviewInteractionRepository.ReviewAccess requireReview(UUID id, boolean lock) {
        return repository.findReview(id, lock).filter(ReviewInteractionRepository.ReviewAccess::published)
                .orElseThrow(() -> new ReviewInteractionException(NOT_FOUND));
    }

    private ReviewComment requireComment(UUID reviewId, UUID commentId) {
        return repository.findComment(reviewId, commentId).orElseThrow(() -> new ReviewInteractionException(NOT_FOUND));
    }
}
