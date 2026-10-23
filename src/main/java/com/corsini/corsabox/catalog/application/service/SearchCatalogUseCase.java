package com.corsini.corsabox.catalog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.CatalogItem;

@Service
public class SearchCatalogUseCase {
    private static final int MAX_QUERY = 100;
    private static final int MAX_PAGE = 500; // limite da TMDB

    private final CatalogProvider provider;

    public SearchCatalogUseCase(CatalogProvider provider) {
        this.provider = provider;
    }

    public List<CatalogItem> execute(String query, int page) {
        var normalized = query.strip().toLowerCase();
        if (normalized.isEmpty()) return List.of();
        if (normalized.length() > MAX_QUERY)
            throw new IllegalArgumentException("Query too long");
        return provider.search(normalized, Math.min(Math.max(page, 1), MAX_PAGE));
    }
}