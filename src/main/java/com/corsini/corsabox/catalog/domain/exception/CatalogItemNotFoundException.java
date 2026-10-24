package com.corsini.corsabox.catalog.domain.exception;

import com.corsini.corsabox.catalog.domain.MediaFormat;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(MediaFormat format, int id) {
        super("Catalog item not found: " + format.path() + "/" + id);
    }
}