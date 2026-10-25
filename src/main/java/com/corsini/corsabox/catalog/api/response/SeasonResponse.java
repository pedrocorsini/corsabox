package com.corsini.corsabox.catalog.api.response;

import java.time.LocalDate;
import java.util.List;

import com.corsini.corsabox.catalog.domain.SeasonDetails;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record SeasonResponse(int number, String name, List<Episode> episodes) {
    public record Episode(int number, String name, String overview, LocalDate airDate,
                          Integer runtimeMinutes, String stillUrl) {}

    public static SeasonResponse from(SeasonDetails d) {
        return new SeasonResponse(d.number(), d.name(), d.episodes().stream()
                .map(e -> new Episode(e.number(), e.name(), e.overview(), e.airDate(),
                        e.runtimeMinutes(), e.stillUrl()))
                .toList());
    }
}