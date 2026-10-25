package com.corsini.corsabox.identity.infrastructure.security;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.application.port.LoginAttemptPolicy;
import com.corsini.corsabox.identity.domain.exception.TooManyLoginAttemptsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
public class CaffeineLoginAttemptPolicy implements LoginAttemptPolicy {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Window(long start, int failures) {}

    private final Cache<String, Window> windows = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW)
            .maximumSize(100_000)
            .build();

    @Override
    public void checkAllowed(String email) {
        var w = windows.getIfPresent(email);
        if (w != null
                && System.currentTimeMillis() - w.start() < WINDOW.toMillis()
                && w.failures() >= MAX_FAILURES)
            throw new TooManyLoginAttemptsException();
    }

    @Override
    public void recordFailure(String email) {
        long now = System.currentTimeMillis();
        windows.asMap().compute(email, (k, cur) ->
                cur == null || now - cur.start() >= WINDOW.toMillis()
                        ? new Window(now, 1)
                        : new Window(cur.start(), cur.failures() + 1));
    }

    @Override
    public void reset(String email) {
        windows.invalidate(email);
    }
}