package com.corsini.corsabox.library.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.corsini.corsabox.catalog.api.response.MediaDetailsResponse;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record LibraryEntryResponse(UUID id, MediaDetailsResponse media, String status,
                                   BigDecimal rating, LocalDate watchedOn, String review,
                                   Instant addedAt, Instant updatedAt) {
    public static LibraryEntryResponse from(LibraryEntry e) {
        var m = e.getMedia();
        var media = new MediaDetailsResponse(m.tmdbId(), m.title(),
                m.kind().name().toLowerCase(Locale.ROOT), m.format().path(), m.year(),
                null, m.posterUrl(), m.backdropUrl(),
                m.releaseDate(), m.runtimeMinutes(), m.genres(),
                List.of(), List.of(), List.of(), List.of(), null, List.of());
        return new LibraryEntryResponse(
            e.getId(),
            media,
            e.getStatus().name().toLowerCase(Locale.ROOT),
            e.getRating() == null ? null : e.getRating().value(),
            e.getWatchedOn(),
            e.getReview(),
            e.getCreatedAt(),
            e.getUpdatedAt());
    }
}