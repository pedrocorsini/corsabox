package com.corsini.corsabox.library.infrastructure.catalog;

import java.util.List;

import org.springframework.stereotype.Component;

import com.corsini.corsabox.catalog.application.service.GetMediaDetailsUseCase;
import com.corsini.corsabox.catalog.application.service.GetSeasonUseCase;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.SeasonSummary;
import com.corsini.corsabox.library.application.port.EpisodeLookup;

@Component
public class CatalogEpisodeLookup implements EpisodeLookup {
    private final GetSeasonUseCase getSeason;
    private final GetMediaDetailsUseCase getDetails;

    public CatalogEpisodeLookup(GetSeasonUseCase getSeason, GetMediaDetailsUseCase getDetails) {
        this.getSeason = getSeason;
        this.getDetails = getDetails;
    }

    @Override
    public List<Episode> season(int tvId, int season) {
        return getSeason.execute(tvId, season).episodes().stream()
                .map(e -> new Episode(e.number(), e.airDate())).toList();
    }

    @Override
    public List<Integer> seasonNumbers(int tvId) {
        return getDetails.execute(MediaFormat.TV, tvId).seasons().stream()
                .map(SeasonSummary::number).toList();
    }
}