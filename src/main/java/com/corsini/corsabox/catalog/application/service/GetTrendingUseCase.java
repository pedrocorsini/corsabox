package com.corsini.corsabox.catalog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.CatalogItem;

@Service
public class GetTrendingUseCase {
    private final CatalogProvider provider;

    public GetTrendingUseCase(CatalogProvider provider) {
        this.provider = provider;
    }

    public List<CatalogItem> execute() {
        return provider.trending();
    }
}