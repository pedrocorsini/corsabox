package com.corsini.corsabox.catalog.api.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.corsini.corsabox.catalog.domain.MediaDetails;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MediaDetailsResponse(int id, String title, String kind, String format, Integer year,
                                   String overview, String posterUrl, String backdropUrl,
                                   LocalDate releaseDate, Integer runtimeMinutes, List<String> genres,
                                   List<String> directors, List<String> writers,
                                   List<String> creators, List<String> cast, String trailerKey, List<SeasonSummaryResponse> seasons) {
    public static MediaDetailsResponse from(MediaDetails d) {
        var i = d.item();
        return new MediaDetailsResponse(
            i.id(), i.title(), i.kind().name().toLowerCase(Locale.ROOT), i.format().path(),
            i.year(), i.overview(), i.posterUrl(), i.backdropUrl(),
            d.releaseDate(), d.runtimeMinutes(), d.genres(),
            d.directors(), d.writers(), d.creators(), d.cast(), d.trailerKey(),
            d.seasons().stream().map(SeasonSummaryResponse::from).toList());
    }
}