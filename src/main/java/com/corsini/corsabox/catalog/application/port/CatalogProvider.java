package com.corsini.corsabox.catalog.application.port;

import java.util.List;

import com.corsini.corsabox.catalog.domain.BrowseCategory;
import com.corsini.corsabox.catalog.domain.BrowseSort;
import com.corsini.corsabox.catalog.domain.CatalogItem;
import com.corsini.corsabox.catalog.domain.MediaDetails;
import com.corsini.corsabox.catalog.domain.MediaFormat;
import com.corsini.corsabox.catalog.domain.TrendingWindow;

public interface CatalogProvider {
    List<CatalogItem> search(String query, int page);
    List<CatalogItem> browse(BrowseCategory category, BrowseSort sort, int page);
    MediaDetails details(MediaFormat format, int id);
    List<CatalogItem> trending(TrendingWindow window);
}