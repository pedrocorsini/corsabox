package com.corsini.corsabox.catalog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.BrowseCategory;
import com.corsini.corsabox.catalog.domain.BrowseSort;
import com.corsini.corsabox.catalog.domain.CatalogItem;

@Service
public class BrowseCatalogUseCase {
    private static final int MAX_PAGE = 500; // limite da TMDB

    private final CatalogProvider provider;

    public BrowseCatalogUseCase(CatalogProvider provider) {
        this.provider = provider;
    }

    public List<CatalogItem> execute(BrowseCategory category, BrowseSort sort, int page) {
        return provider.browse(category, sort, Math.min(Math.max(page, 1), MAX_PAGE));
    }
}