package com.corsini.corsabox.identity.api;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.corsini.corsabox.identity.application.dto.GoogleIdentity;
import jakarta.servlet.http.HttpSession;

/** Short-lived browser-bound identity, never an application session or a URL token. */
public record GoogleLoginFlow(GoogleIdentity identity, Instant expiresAt) {
    public static final String ATTRIBUTE = GoogleLoginFlow.class.getName();

    public static GoogleLoginFlow require(HttpSession session) {
        if (session != null && session.getAttribute(ATTRIBUTE) instanceof GoogleLoginFlow flow
                && Instant.now().isBefore(flow.expiresAt())) return flow;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google login expired. Start again.");
    }
}
