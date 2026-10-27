package com.corsini.corsabox.catalog.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.corsini.corsabox.catalog.api.response.CatalogItemResponse;
import com.corsini.corsabox.catalog.api.response.MediaDetailsResponse;
import com.corsini.corsabox.catalog.api.response.MediaGenreResponse;
import com.corsini.corsabox.catalog.api.response.SeasonResponse;
import com.corsini.corsabox.catalog.api.response.WatchAvailabilityResponse;
import com.corsini.corsabox.catalog.application.service.BrowseCatalogUseCase;
import com.corsini.corsabox.catalog.application.service.GetGenreUseCase;
import com.corsini.corsabox.catalog.application.service.GetMediaDetailsUseCase;
import com.corsini.corsabox.catalog.application.service.GetSeasonUseCase;
import com.corsini.corsabox.catalog.application.service.GetTrendingUseCase;
import com.corsini.corsabox.catalog.application.service.GetWatchAvailabilityUseCase;
import com.corsini.corsabox.catalog.application.service.SearchCatalogUseCase;
import com.corsini.corsabox.catalog.domain.BrowseCategory;
import com.corsini.corsabox.catalog.domain.BrowseSort;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.TrendingWindow;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final GetTrendingUseCase getTrending;
    private final SearchCatalogUseCase searchCatalog;
    private final GetMediaDetailsUseCase getDetails;
    private final BrowseCatalogUseCase browseCatalog;
    private final GetSeasonUseCase getSeason;
    private final GetGenreUseCase getGenres;
    private final GetWatchAvailabilityUseCase getWatchAvailability;

    public CatalogController(GetTrendingUseCase getTrending, SearchCatalogUseCase searchCatalog,
            GetMediaDetailsUseCase getDetails, BrowseCatalogUseCase browseCatalog, GetSeasonUseCase getSeason,
            GetGenreUseCase getGenres, GetWatchAvailabilityUseCase getWatchAvailability) {
        this.getTrending = getTrending;
        this.searchCatalog = searchCatalog;
        this.getDetails = getDetails;
        this.browseCatalog = browseCatalog;
        this.getSeason = getSeason;
        this.getGenres = getGenres;
        this.getWatchAvailability = getWatchAvailability;
    }

    @GetMapping("/trending")
    public List<CatalogItemResponse> trending(@RequestParam(defaultValue = "week") String window) {
        return getTrending.execute(TrendingWindow.from(window)).stream().map(CatalogItemResponse::from).toList();
    }

    @GetMapping("/search")
    public List<CatalogItemResponse> search(@RequestParam String q,
            @RequestParam(defaultValue = "1") int page) {
        return searchCatalog.execute(q, page).stream().map(CatalogItemResponse::from).toList();
    }

    @GetMapping("/{format}/{id}")
    public MediaDetailsResponse details(@PathVariable String format, @PathVariable int id) {
        return MediaDetailsResponse.from(getDetails.execute(MediaFormat.from(format), id));
    }

    @GetMapping("/{format}/{id}/watch-providers")
    public WatchAvailabilityResponse watchProviders(@PathVariable String format, @PathVariable int id) {
        return WatchAvailabilityResponse.from(getWatchAvailability.execute(MediaFormat.from(format), id));
    }

    @GetMapping("/browse")
    public List<CatalogItemResponse> browse(@RequestParam String category,
            @RequestParam(defaultValue = "popularity") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) List<Integer> genreIds) {
        return browseCatalog.execute(BrowseCategory.from(category), BrowseSort.from(sort), page, genreIds)
                .stream().map(CatalogItemResponse::from).toList();
    }

    @GetMapping("/tv/{id}/seasons/{season}")
    public SeasonResponse season(@PathVariable int id, @PathVariable int season) {
        return SeasonResponse.from(getSeason.execute(id, season));
    }

    @GetMapping("/genres")
    public List<MediaGenreResponse> genres(@RequestParam String format) {
        return getGenres.execute(MediaFormat.from(format)).stream().map(MediaGenreResponse::from).toList();
    }
}
