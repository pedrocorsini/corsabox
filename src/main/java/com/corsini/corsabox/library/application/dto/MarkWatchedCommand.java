package com.corsini.corsabox.library.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.corsini.corsabox.catalog.domain.MediaFormat;

public record MarkWatchedCommand(UUID userId, MediaFormat format, int tmdbId,
                                 BigDecimal rating, LocalDate watchedOn, String review,
                                 Boolean publishToFeed, Boolean commentsEnabled) {}
