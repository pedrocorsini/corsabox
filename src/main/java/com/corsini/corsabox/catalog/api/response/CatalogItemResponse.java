package com.corsini.corsabox.catalog.api.response;

import java.util.Locale;

import com.corsini.corsabox.catalog.domain.CatalogItem;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record CatalogItemResponse(int id, String title, String kind, Integer year,
                                  String posterUrl, String backdropUrl) {
    public static CatalogItemResponse from(CatalogItem item) {
        return new CatalogItemResponse(
            item.id(),
            item.title(),
            item.kind().name().toLowerCase(Locale.ROOT),
            item.year(),
            item.posterUrl(),
            item.backdropUrl());
    }
}