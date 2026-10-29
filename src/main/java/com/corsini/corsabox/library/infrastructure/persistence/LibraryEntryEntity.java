package com.corsini.corsabox.library.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaKind;
import com.corsini.corsabox.library.domain.EntryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "library_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_library_user_media",
                columnNames = {"userId", "tmdbId", "format"}),
        indexes = {@Index(name = "idx_library_user_status", columnList = "userId,status"),
                @Index(name = "idx_library_feed", columnList = "publishedAt DESC,id DESC")})
@Getter
@Setter
@NoArgsConstructor
public class LibraryEntryEntity {
        @Id
        private UUID id;

        @Column(nullable = false)
        private UUID userId;

        @Column(nullable = false)
        private int tmdbId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 10)
        private MediaFormat format;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 10)
        private MediaKind kind;

        @Column(nullable = false)
        private String title;

        @Column
        private Integer releaseYear;

        @Column
        private String posterUrl;

        @Column
        private String backdropUrl;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 10)
        private EntryStatus status;

        @Column
        private Integer ratingHalves;

        @Column
        private LocalDate watchedOn;

        @Column(length = 5000)
        private String review;

        private Instant publishedAt;

        @Column(nullable = false, columnDefinition = "boolean default true")
        private boolean commentsEnabled = true;

        @Column(nullable = false, updatable = false)
        private Instant createdAt;

        @Column(nullable = false)
        private Instant updatedAt;
    
        @Column
        private LocalDate releaseDate;

        @Column
        private Integer runtimeMinutes;

        @Column(length = 500)
        private String genres;   
}
