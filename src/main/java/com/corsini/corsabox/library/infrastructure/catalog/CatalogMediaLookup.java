package com.corsini.corsabox.library.infrastructure.catalog;

import org.springframework.stereotype.Component;

import com.corsini.corsabox.catalog.application.service.GetMediaDetailsUseCase;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.library.application.port.MediaLookup;
import com.corsini.corsabox.library.domain.MediaSnapshot;

@Component
public class CatalogMediaLookup implements MediaLookup {
    private final GetMediaDetailsUseCase getDetails;

    public CatalogMediaLookup(GetMediaDetailsUseCase getDetails) {
        this.getDetails = getDetails;
    }

    @Override
    public MediaSnapshot find(MediaFormat format, int tmdbId) {
        var d = getDetails.execute(format, tmdbId);
        var i = d.item();
        return new MediaSnapshot(i.id(), i.format(), i.kind(), i.title(), i.year(),
                d.releaseDate(), d.runtimeMinutes(), d.genres(), i.posterUrl(), i.backdropUrl());
    }
}