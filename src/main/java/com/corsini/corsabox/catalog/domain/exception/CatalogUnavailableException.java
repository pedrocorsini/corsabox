package com.corsini.corsabox.catalog.domain.exception;

public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(Throwable cause) {
        super("Catalog provider unavailable", cause);
    }
}