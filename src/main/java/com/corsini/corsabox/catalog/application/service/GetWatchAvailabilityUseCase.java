package com.corsini.corsabox.catalog.application.service;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.WatchAvailability;

@Service
public class GetWatchAvailabilityUseCase {
    private final CatalogProvider provider;

    public GetWatchAvailabilityUseCase(CatalogProvider provider) {
        this.provider = provider;
    }

    public WatchAvailability execute(MediaFormat format, int id) {
        return provider.watchAvailability(format, id);
    }
}
