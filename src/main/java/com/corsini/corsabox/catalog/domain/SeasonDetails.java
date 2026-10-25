package com.corsini.corsabox.catalog.domain;

import java.util.List;

public record SeasonDetails(int number, String name, List<EpisodeItem> episodes) {}