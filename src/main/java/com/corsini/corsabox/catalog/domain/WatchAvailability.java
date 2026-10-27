package com.corsini.corsabox.catalog.domain;

import java.util.List;

import com.corsini.corsabox.catalog.application.port.WatchProvider;

public record WatchAvailability(String link, boolean inTheaters, List<WatchProvider> streaming, List<WatchProvider> free,
                                List<WatchProvider> ads, List<WatchProvider> rent, List<WatchProvider> buy) {}
