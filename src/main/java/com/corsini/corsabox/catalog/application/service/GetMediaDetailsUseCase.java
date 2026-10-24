package com.corsini.corsabox.catalog.application.service;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.CatalogItem;
import com.corsini.corsabox.catalog.domain.MediaFormat;

@Service
public class GetMediaDetailsUseCase {
    private final CatalogProvider provider;

    public GetMediaDetailsUseCase(CatalogProvider provider) {
        this.provider = provider;
    }

    public CatalogItem execute(MediaFormat format, int id) {
        return provider.details(format, id);
    }
}