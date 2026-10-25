package com.corsini.corsabox.catalog.domain;

import java.time.LocalDate;

public record EpisodeItem(int number, String name, String overview, LocalDate airDate,
                          Integer runtimeMinutes, String stillUrl) {}