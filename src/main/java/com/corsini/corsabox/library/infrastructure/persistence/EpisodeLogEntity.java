package com.corsini.corsabox.library.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "episode_logs",
        uniqueConstraints = @UniqueConstraint(name = "uk_episode_log",
                columnNames = {"entryId", "seasonNumber", "episodeNumber"}),
        indexes = @Index(name = "idx_episode_log_entry", columnList = "entryId"))
@Getter @Setter @NoArgsConstructor
public class EpisodeLogEntity {
    @Id private UUID id;
    @Column(nullable = false) private UUID entryId;
    @Column(nullable = false) private int seasonNumber;
    @Column(nullable = false) private int episodeNumber;
    @Column private Integer ratingHalves;
    @Column private LocalDate watchedOn;
    @Column(length = 5000) private String review;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
}