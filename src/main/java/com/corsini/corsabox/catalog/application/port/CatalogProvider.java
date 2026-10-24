package com.corsini.corsabox.catalog.application.port;

import java.util.List;

import com.corsini.corsabox.catalog.domain.BrowseCategory;
import com.corsini.corsabox.catalog.domain.BrowseSort;
import com.corsini.corsabox.catalog.domain.CatalogItem;
import com.corsini.corsabox.catalog.domain.MediaFormat;

public interface CatalogProvider {
    List<CatalogItem> trending();
    List<CatalogItem> search(String query, int page);
    CatalogItem details(MediaFormat format, int id);
    List<CatalogItem> browse(BrowseCategory category, BrowseSort sort, int page);
}