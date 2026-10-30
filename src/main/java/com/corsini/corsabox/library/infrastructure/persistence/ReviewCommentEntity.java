package com.corsini.corsabox.library.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.corsini.corsabox.identity.infrastructure.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_comments", indexes = {
    @Index(name = "idx_review_comments_page", columnList = "review_id,created_at,id"),
    @Index(name = "idx_episode_comments_page", columnList = "episode_review_id,created_at,id"),
    @Index(name = "idx_review_comments_author", columnList = "author_id")
}, check = @CheckConstraint(name = "ck_review_comment_target", constraint = "(review_id is not null and episode_review_id is null) or (review_id is null and episode_review_id is not null)"))
@Getter @Setter @NoArgsConstructor
public class ReviewCommentEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LibraryEntryEntity review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_review_id", foreignKey = @ForeignKey(name = "fk_comment_episode_review"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EpisodeLogEntity episodeReview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity author;

    @Column(nullable = false, length = 2000) private String text;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
}
