package com.corsini.corsabox.catalog.domain;

import java.util.Locale;

public enum BrowseSort {
    POPULARITY, TITLE;

    public static BrowseSort from(String value) {
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown sort: " + value);
        }
    }
}