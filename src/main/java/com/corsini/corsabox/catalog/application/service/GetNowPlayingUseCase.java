package com.corsini.corsabox.catalog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.corsini.corsabox.catalog.application.port.CatalogProvider;
import com.corsini.corsabox.catalog.domain.CatalogItem;

@Service
public class GetNowPlayingUseCase {
    private static final int MAX_PAGE = 500;
    private final CatalogProvider provider;

    public GetNowPlayingUseCase(CatalogProvider provider) {
        this.provider = provider;
    }

    public List<CatalogItem> execute(int page, List<Integer> genreIds) {
        var normalizedGenreIds = genreIds == null ? List.<Integer>of()
                : genreIds.stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();
        return provider.nowPlaying(Math.min(Math.max(page, 1), MAX_PAGE), normalizedGenreIds);
    }
}
