package com.corsini.corsabox.catalog.domain;

public enum TrendingWindow {
    DAY("day"), WEEK("week");

    private final String path;

    TrendingWindow(String path) { this.path = path; }

    public String path() { return path; }

    public static TrendingWindow from(String value) {
        for (var w : values())
            if (w.path.equalsIgnoreCase(value)) return w;
        throw new IllegalArgumentException("Unknown window: " + value);
    }
}