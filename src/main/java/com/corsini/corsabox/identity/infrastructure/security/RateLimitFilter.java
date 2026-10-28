
package com.corsini.corsabox.identity.infrastructure.security;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String path, int limit, Duration window) {}
    private record Window(long start, int count, long lengthMs) {}

    private static final List<Rule> RULES = List.of(
        new Rule("/api/auth/register", 10, Duration.ofHours(1)),
        new Rule("/api/auth/register/confirm", 20, Duration.ofMinutes(15)),
        new Rule("/api/auth/forgot-password", 5, Duration.ofHours(1)),
        new Rule("/api/auth/login", 10, Duration.ofMinutes(1)),
        new Rule("/api/auth/google/complete", 10, Duration.ofMinutes(1)),
        new Rule("/api/auth/refresh", 60, Duration.ofMinutes(1)));

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static Optional<Rule> ruleFor(HttpServletRequest req) {
        if (!HttpMethod.POST.matches(req.getMethod())) return Optional.empty();
        return RULES.stream().filter(r -> r.path().equals(req.getRequestURI())).findFirst();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        var rule = ruleFor(req);
        if (rule.isEmpty()) {
            chain.doFilter(req, res);
            return;
        }

        long retryAfter = tryAcquire(rule.get().path() + "|" + req.getRemoteAddr(), rule.get());
        if (retryAfter > 0) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(retryAfter));
            res.setContentType("application/problem+json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("{\"status\":429,\"detail\":\"Muitas tentativas. Tente novamente mais tarde.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    /** Devolve 0 se liberado, ou os segundos até a janela abrir de novo. */
    private long tryAcquire(String key, Rule rule) {
        long now = System.currentTimeMillis();
        long lengthMs = rule.window().toMillis();
        long[] retryAfter = {0};

        windows.compute(key, (k, cur) -> {
            if (cur == null || now - cur.start() >= lengthMs) return new Window(now, 1, lengthMs);
            if (cur.count() >= rule.limit()) {
                retryAfter[0] = Math.max(1, (cur.start() + lengthMs - now + 999) / 1000);
                return cur;
            }
            return new Window(cur.start(), cur.count() + 1, lengthMs);
        });
        return retryAfter[0];
    }

    @Scheduled(fixedDelayString = "PT5M")
    void evictExpired() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().start() >= e.getValue().lengthMs());
    }
}
