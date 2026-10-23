package com.corsini.corsabox.catalog.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.corsini.corsabox.catalog.api.response.CatalogItemResponse;
import com.corsini.corsabox.catalog.application.service.GetMediaDetailsUseCase;
import com.corsini.corsabox.catalog.application.service.GetTrendingUseCase;
import com.corsini.corsabox.catalog.application.service.SearchCatalogUseCase;
import com.corsini.corsabox.catalog.domain.MediaFormat;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final GetTrendingUseCase getTrending;
    private final SearchCatalogUseCase searchCatalog;
    private final GetMediaDetailsUseCase getDetails;

    public CatalogController(GetTrendingUseCase getTrending, SearchCatalogUseCase searchCatalog,
                             GetMediaDetailsUseCase getDetails) {
        this.getTrending = getTrending;
        this.searchCatalog = searchCatalog;
        this.getDetails = getDetails;
    }

    @GetMapping("/trending")
    public List<CatalogItemResponse> trending() {
        return getTrending.execute().stream().map(CatalogItemResponse::from).toList();
    }

    @GetMapping("/search")
    public List<CatalogItemResponse> search(@RequestParam String q,
                                            @RequestParam(defaultValue = "1") int page) {
        return searchCatalog.execute(q, page).stream().map(CatalogItemResponse::from).toList();
    }

    @GetMapping("/{format}/{id}")
    public CatalogItemResponse details(@PathVariable String format, @PathVariable int id) {
        return CatalogItemResponse.from(getDetails.execute(MediaFormat.from(format), id));
    }
}