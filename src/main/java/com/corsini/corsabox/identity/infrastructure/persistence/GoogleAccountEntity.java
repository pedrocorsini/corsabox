package com.corsini.corsabox.identity.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "google_accounts")
@Getter
@Setter
@NoArgsConstructor
public class GoogleAccountEntity {
    @Id
    @Column(length = 255)
    private String subject;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;
}
