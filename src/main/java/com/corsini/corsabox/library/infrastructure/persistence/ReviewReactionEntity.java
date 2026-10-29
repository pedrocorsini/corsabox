package com.corsini.corsabox.library.infrastructure.persistence;

import java.util.UUID;

import org.hibernate.annotations.Check;
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
    @UniqueConstraint(name = "uk_comment_reaction_user", columnNames = {"comment_id", "user_id"})
}, indexes = @Index(name = "idx_review_reactions_user", columnList = "user_id"))
@Check(constraints = "(review_id is not null and comment_id is null) or (review_id is null and comment_id is not null)")
@Getter @Setter @NoArgsConstructor
public class ReviewReactionEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LibraryEntryEntity review;

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
