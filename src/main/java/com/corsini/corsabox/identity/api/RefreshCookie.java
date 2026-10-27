package com.corsini.corsabox.identity.api;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.corsini.corsabox.identity.application.dto.AuthTokens;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class RefreshCookie {
    public static final String NAME = "corsabox_refresh";

    private final boolean secure;
    private final String sameSite;

    public RefreshCookie(@Value("${app.refresh-cookie.secure}") boolean secure,
                         @Value("${app.refresh-cookie.same-site}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void write(HttpServletResponse res, AuthTokens tokens) {
        res.addHeader(HttpHeaders.SET_COOKIE, build(tokens.refreshToken(), tokens.refreshTtl()));
    }

    public void clear(HttpServletResponse res) {
        res.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO));
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/auth")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}