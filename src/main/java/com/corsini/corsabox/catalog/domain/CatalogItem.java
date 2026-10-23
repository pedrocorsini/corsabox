package com.corsini.corsabox.catalog.domain;

public record CatalogItem(int id, String title, MediaKind kind, MediaFormat format, Integer year,
                          String posterUrl, String backdropUrl) {}