package com.corsini.corsabox.catalog.domain;

import java.time.LocalDate;

public record SeasonSummary(int number, String name, int episodeCount, LocalDate airDate) {}
