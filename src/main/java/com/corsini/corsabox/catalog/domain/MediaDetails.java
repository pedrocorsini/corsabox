package com.corsini.corsabox.catalog.domain;

import java.time.LocalDate;
import java.util.List;

public record MediaDetails(CatalogItem item, LocalDate releaseDate, Integer runtimeMinutes,
                           List<String> genres, List<String> directors, List<String> writers,
                           List<String> creators, List<String> cast) {}