package com.corsini.corsabox.library.domain;

import java.time.LocalDate;
import java.util.List;

import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.MediaKind;

public record MediaSnapshot(int tmdbId, MediaFormat format, MediaKind kind, String title, Integer year,
                            LocalDate releaseDate, Integer runtimeMinutes, List<String> genres,
                            String posterUrl, String backdropUrl) {
    public MediaSnapshot {
        genres = genres == null ? List.of() : List.copyOf(genres);
    }

    public boolean hasMetadata() {
        return releaseDate != null || runtimeMinutes != null || !genres.isEmpty();
    }
}