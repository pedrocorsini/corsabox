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
    @Index(name = "idx_review_comments_author", columnList = "author_id")
})
@Getter @Setter @NoArgsConstructor
public class ReviewCommentEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LibraryEntryEntity review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity author;

    @Column(nullable = false, length = 2000) private String text;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
}
