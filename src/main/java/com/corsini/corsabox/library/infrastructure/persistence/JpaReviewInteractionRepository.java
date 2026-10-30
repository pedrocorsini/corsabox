package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.corsini.corsabox.identity.infrastructure.persistence.UserEntity;
import com.corsini.corsabox.library.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Repository
public class JpaReviewInteractionRepository implements ReviewInteractionRepository {
    private final EntityManager em;

    public JpaReviewInteractionRepository(EntityManager em) { this.em = em; }

    @Override
    public Optional<ReviewAccess> findReview(UUID id, boolean lock) {
        var entry = lock ? em.find(LibraryEntryEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(LibraryEntryEntity.class, id);
        if (entry != null) return Optional.of(new ReviewAccess(entry.getPublishedAt() != null
                && entry.getReview() != null && entry.getStatus() == EntryStatus.WATCHED, entry.isCommentsEnabled()));
        var episode = lock ? em.find(EpisodeLogEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(EpisodeLogEntity.class, id);
        return Optional.ofNullable(episode).map(e -> new ReviewAccess(e.getPublishedAt() != null
                && e.getReview() != null, e.isCommentsEnabled()));
    }

    @Override
    public Page<ReviewComment> comments(UUID reviewId, Pageable pageable) {
        var rows = em.createQuery("""
                select c from ReviewCommentEntity c join fetch c.author
                where (c.review.id = :review or c.episodeReview.id = :review) order by c.createdAt desc, c.id desc
                """, ReviewCommentEntity.class).setParameter("review", reviewId)
                .setFirstResult(Math.toIntExact(pageable.getOffset())).setMaxResults(pageable.getPageSize())
                .getResultList().stream().map(this::toDomain).toList();
        var total = em.createQuery("select count(c) from ReviewCommentEntity c where c.review.id = :review or c.episodeReview.id = :review", Long.class)
                .setParameter("review", reviewId).getSingleResult();
        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public Optional<ReviewComment> findComment(UUID reviewId, UUID commentId) {
        return em.createQuery("""
                select c from ReviewCommentEntity c join fetch c.author
                where c.id = :id and (c.review.id = :review or c.episodeReview.id = :review)
                """, ReviewCommentEntity.class).setParameter("id", commentId).setParameter("review", reviewId)
                .getResultStream().findFirst().map(this::toDomain);
    }

    @Override
    public ReviewComment saveComment(ReviewComment comment) {
        var entity = new ReviewCommentEntity();
        entity.setId(comment.id());
        if (em.find(LibraryEntryEntity.class, comment.reviewId()) != null)
            entity.setReview(em.getReference(LibraryEntryEntity.class, comment.reviewId()));
        else entity.setEpisodeReview(em.getReference(EpisodeLogEntity.class, comment.reviewId()));
        entity.setAuthor(em.getReference(UserEntity.class, comment.authorId()));
        entity.setText(comment.text());
        entity.setCreatedAt(comment.createdAt());
        em.persist(entity);
        return comment;
    }

    @Override
    public void deleteComment(UUID commentId) {
        em.remove(em.getReference(ReviewCommentEntity.class, commentId));
    }

    @Override
    public void setReaction(UUID reviewId, UUID commentId, UUID userId, Reaction reaction) {
        var target = commentId == null ? "(r.review.id = :target or r.episodeReview.id = :target)" : "r.comment.id = :target";
        var existing = em.createQuery("select r from ReviewReactionEntity r where " + target
                + " and r.user.id = :user", ReviewReactionEntity.class)
                .setParameter("target", commentId == null ? reviewId : commentId).setParameter("user", userId)
                .getResultStream().findFirst();
        if (existing.isPresent()) {
            if (reaction == null) em.remove(existing.get());
            else existing.get().setValue(reaction);
        } else if (reaction != null) {
            var entity = new ReviewReactionEntity();
            entity.setId(UUID.randomUUID());
            entity.setUser(em.getReference(UserEntity.class, userId));
            if (commentId == null) {
                if (em.find(LibraryEntryEntity.class, reviewId) != null)
                    entity.setReview(em.getReference(LibraryEntryEntity.class, reviewId));
                else entity.setEpisodeReview(em.getReference(EpisodeLogEntity.class, reviewId));
            }
            else entity.setComment(em.getReference(ReviewCommentEntity.class, commentId));
            entity.setValue(reaction);
            em.persist(entity);
        }
    }

    @Override
    public Map<UUID, Activity> activities(Collection<UUID> reviewIds, UUID viewerId) {
        var result = new HashMap<UUID, Activity>();
        if (reviewIds.isEmpty()) return result;
        reactionSummaries(reviewIds, viewerId, false).forEach((id, reactions) -> result.put(id, new Activity(reactions, 0)));
        var counts = em.createQuery("""
                select coalesce(c.review.id, c.episodeReview.id), count(c) from ReviewCommentEntity c
                where c.review.id in :ids or c.episodeReview.id in :ids group by coalesce(c.review.id, c.episodeReview.id)
                """, Object[].class).setParameter("ids", reviewIds).getResultList();
        counts.forEach(row -> {
            var id = (UUID) row[0];
            result.put(id, new Activity(result.getOrDefault(id, Activity.EMPTY).reactions(), (Long) row[1]));
        });
        return result;
    }

    @Override
    public Map<UUID, ReactionSummary> commentReactions(Collection<UUID> commentIds, UUID viewerId) {
        return reactionSummaries(commentIds, viewerId, true);
    }

    private Map<UUID, ReactionSummary> reactionSummaries(Collection<UUID> ids, UUID viewerId, boolean comments) {
        var result = new HashMap<UUID, ReactionSummary>();
        if (ids.isEmpty()) return result;
        var target = comments ? "r.comment.id" : "coalesce(r.review.id, r.episodeReview.id)";
        var filter = comments ? "r.comment.id in :ids" : "(r.review.id in :ids or r.episodeReview.id in :ids)";
        var rows = em.createQuery("select " + target + ", r.value, count(r), "
                + "sum(case when r.user.id = :viewer then 1 else 0 end) from ReviewReactionEntity r where "
                + filter + " group by " + target + ", r.value", Object[].class)
                .setParameter("viewer", viewerId).setParameter("ids", ids).getResultList();
        rows.forEach(row -> {
            var id = (UUID) row[0];
            var value = (Reaction) row[1];
            var previous = result.getOrDefault(id, ReactionSummary.EMPTY);
            var count = (Long) row[2];
            result.put(id, new ReactionSummary(value == Reaction.LIKE ? count : previous.likes(),
                    value == Reaction.DISLIKE ? count : previous.dislikes(),
                    (Long) row[3] > 0 ? value : previous.myReaction()));
        });
        return result;
    }

    private ReviewComment toDomain(ReviewCommentEntity c) {
        return new ReviewComment(c.getId(), c.getReview() == null ? c.getEpisodeReview().getId() : c.getReview().getId(), c.getAuthor().getId(),
                c.getAuthor().getUsername(), c.getText(), c.getCreatedAt());
    }
}
