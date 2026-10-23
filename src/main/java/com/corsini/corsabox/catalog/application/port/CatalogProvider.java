package com.corsini.corsabox.catalog.application.port;

import java.util.List;

import com.corsini.corsabox.catalog.domain.CatalogItem;

public interface CatalogProvider {
    List<CatalogItem> trending();
    List<CatalogItem> search(String query, int page);
}