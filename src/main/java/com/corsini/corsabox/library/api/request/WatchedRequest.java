package com.corsini.corsabox.library.api.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/** Todos os campos são opcionais. Ausente ou nulo limpa o valor. */
public record WatchedRequest(
    @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
    LocalDate watchedOn,
    @Size(max = 5000) String review,
    Boolean publishToFeed,
    Boolean commentsEnabled
) {}
