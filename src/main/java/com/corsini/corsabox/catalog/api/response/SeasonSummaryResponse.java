package com.corsini.corsabox.catalog.api.response;

import java.time.LocalDate;

import com.corsini.corsabox.catalog.domain.SeasonSummary;

public record SeasonSummaryResponse(int number, String name, int episodeCount, LocalDate airDate) {
    public static SeasonSummaryResponse from(SeasonSummary s) {
        return new SeasonSummaryResponse(s.number(), s.name(), s.episodeCount(), s.airDate());
    }
}
