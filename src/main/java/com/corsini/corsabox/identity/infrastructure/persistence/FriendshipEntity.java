package com.corsini.corsabox.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.corsini.corsabox.identity.domain.Friendship.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(name = "uk_friendship_pair",
        columnNames = {"first_user_id", "second_user_id"}), indexes = {
    @Index(name = "idx_friendship_first_status", columnList = "first_user_id,status"),
    @Index(name = "idx_friendship_second_status", columnList = "second_user_id,status")
})
@Check(constraints = "first_user_id < second_user_id and (requested_by = first_user_id or requested_by = second_user_id)")
@Getter @Setter @NoArgsConstructor
public class FriendshipEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "first_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity firstUser;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "second_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity secondUser;
    @Column(name = "requested_by", nullable = false) private UUID requestedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8) private Status status;
    @Column(nullable = false, updatable = false) private Instant createdAt;
}
