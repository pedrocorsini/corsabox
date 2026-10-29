package com.corsini.corsabox.identity.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "public_profiles")
@Getter @Setter @NoArgsConstructor
public class PublicProfileEntity {
    @Id private UUID userId;
    @Column(length = 300, nullable = false) private String bio = "";
    @Column(columnDefinition = "text", nullable = false) private String photo = "";
    @ElementCollection
    @CollectionTable(name = "profile_favorites", joinColumns = @JoinColumn(name = "user_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entry_id"}))
    @OrderColumn(name = "position")
    @Column(name = "entry_id", nullable = false)
    private List<UUID> favorites = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "profile_catalog_favorites", joinColumns = @JoinColumn(name = "user_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "format", "media_id"}))
    @OrderColumn(name = "position")
    private List<FavoriteMedia> catalogFavorites = new ArrayList<>();
}
