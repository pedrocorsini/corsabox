package com.corsini.corsabox.library.infrastructure.catalog;

import java.util.List;

import org.springframework.stereotype.Component;

import com.corsini.corsabox.catalog.application.service.GetSeasonUseCase;
import com.corsini.corsabox.library.application.port.EpisodeLookup;

@Component
public class CatalogEpisodeLookup implements EpisodeLookup {
    private final GetSeasonUseCase getSeason;

    public CatalogEpisodeLookup(GetSeasonUseCase getSeason) { this.getSeason = getSeason; }

    @Override
    public List<Episode> season(int tvId, int season) {
        return getSeason.execute(tvId, season).episodes().stream()
                .map(e -> new Episode(e.number(), e.airDate())).toList();
    }
}