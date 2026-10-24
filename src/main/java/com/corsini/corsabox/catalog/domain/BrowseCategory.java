package com.corsini.corsabox.catalog.domain;

import java.util.Locale;

public enum BrowseCategory {
    MOVIES, TV, ANIME;

    public static BrowseCategory from(String value) {
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown category: " + value);
        }
    }
}