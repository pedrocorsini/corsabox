package com.corsini.corsabox.catalog.application.service;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.SeasonDetails;

@Service
public class GetSeasonUseCase {
    private final CatalogProvider provider;

    public GetSeasonUseCase(CatalogProvider provider) { this.provider = provider; }

    public SeasonDetails execute(int tvId, int season) {
        if (season < 0) throw new IllegalArgumentException("Invalid season");
        return provider.season(tvId, season);
    }
}
