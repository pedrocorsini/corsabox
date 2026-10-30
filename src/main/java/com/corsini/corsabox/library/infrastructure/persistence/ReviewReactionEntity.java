package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.corsini.corsabox.identity.infrastructure.persistence.UserEntity;
import com.corsini.corsabox.library.domain.Reaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_reactions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_review_reaction_user", columnNames = {"review_id", "user_id"}),
    @UniqueConstraint(name = "uk_episode_reaction_user", columnNames = {"episode_review_id", "user_id"}),
    @UniqueConstraint(name = "uk_comment_reaction_user", columnNames = {"comment_id", "user_id"})
}, indexes = @Index(name = "idx_review_reactions_user", columnList = "user_id"),
    check = @CheckConstraint(name = "ck_review_reaction_target", constraint = "(case when review_id is null then 0 else 1 end + case when episode_review_id is null then 0 else 1 end + case when comment_id is null then 0 else 1 end) = 1"))
@Getter @Setter @NoArgsConstructor
public class ReviewReactionEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LibraryEntryEntity review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_review_id", foreignKey = @ForeignKey(name = "fk_reaction_episode_review"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EpisodeLogEntity episodeReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ReviewCommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", nullable = false, length = 7)
    private Reaction value;
}
