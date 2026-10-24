package com.corsini.corsabox.catalog.domain;

public enum MediaFormat {
    MOVIE("movie"), TV("tv");

    private final String path;

    MediaFormat(String path) { this.path = path; }

    public String path() { return path; }

    public static MediaFormat from(String value) {
        for (var f : values())
            if (f.path.equalsIgnoreCase(value)) return f;
        throw new IllegalArgumentException("Unknown media format: " + value);
    }
}