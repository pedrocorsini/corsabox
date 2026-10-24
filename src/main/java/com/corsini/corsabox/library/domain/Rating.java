package com.corsini.corsabox.library.domain;

import java.math.BigDecimal;

public record Rating(int halves) {
    private static final BigDecimal MAX = BigDecimal.valueOf(5);

    public Rating {
        if (halves < 0 || halves > 10)
            throw new IllegalArgumentException("Rating must be between 0 and 5");
    }

    public static Rating of(BigDecimal value) {
        if (value.signum() < 0 || value.compareTo(MAX) > 0)
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        var doubled = value.multiply(BigDecimal.TWO);
        if (doubled.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("Rating must go up in steps of 0.5");
        return new Rating(doubled.intValueExact());
    }

    public BigDecimal value() {
        return BigDecimal.valueOf(halves * 5L, 1);
    }
}