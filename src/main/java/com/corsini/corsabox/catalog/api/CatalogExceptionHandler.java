package com.corsini.corsabox.catalog.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.corsini.corsabox.catalog.domain.exception.CatalogItemNotFoundException;
import com.corsini.corsabox.catalog.domain.exception.CatalogUnavailableException;

@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail handleUnavailable(CatalogUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "O catálogo está indisponível no momento. Tente novamente.");
    }

    @ExceptionHandler(CatalogItemNotFoundException.class)
    ProblemDetail handleNotFound(CatalogItemNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}