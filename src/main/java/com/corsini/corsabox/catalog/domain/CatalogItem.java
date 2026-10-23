package com.corsini.corsabox.catalog.domain;

public record CatalogItem(int id, String title, MediaKind kind, Integer year,
                          String posterUrl, String backdropUrl) {}