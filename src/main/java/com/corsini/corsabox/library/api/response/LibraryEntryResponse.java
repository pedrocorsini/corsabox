package com.corsini.corsabox.library.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import com.corsini.corsabox.catalog.api.response.CatalogItemResponse;
import com.corsini.corsabox.library.domain.LibraryEntry;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record LibraryEntryResponse(UUID id, CatalogItemResponse media, String status,
                                   BigDecimal rating, LocalDate watchedOn, String review,
                                   Instant addedAt, Instant updatedAt) {
    public static LibraryEntryResponse from(LibraryEntry e) {
        var m = e.getMedia();
        var media = new CatalogItemResponse(m.tmdbId(), m.title(),
                m.kind().name().toLowerCase(Locale.ROOT), m.format().path(),
                m.year(), m.posterUrl(), m.backdropUrl());
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