package com.corsini.corsabox.library.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.corsini.corsabox.library.domain.EpisodeLog;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record EpisodeLogResponse(int season, int episode, BigDecimal rating,
                                 LocalDate watchedOn, String review, Instant updatedAt,
                                 Instant publishedAt, boolean commentsEnabled) {
    public static EpisodeLogResponse from(EpisodeLog l) {
        return new EpisodeLogResponse(l.getSeasonNumber(), l.getEpisodeNumber(),
                l.getRating() == null ? null : l.getRating().value(),
                l.getWatchedOn(), l.getReview(), l.getUpdatedAt(), l.getPublishedAt(), l.isCommentsEnabled());
    }
}
