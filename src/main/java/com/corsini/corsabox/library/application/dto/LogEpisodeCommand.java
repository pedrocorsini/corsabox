package com.corsini.corsabox.library.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LogEpisodeCommand(UUID userId, int tmdbId, int season, int episode,
                                BigDecimal rating, LocalDate watchedOn, String review,
                                Boolean publishToFeed, Boolean commentsEnabled) {}
