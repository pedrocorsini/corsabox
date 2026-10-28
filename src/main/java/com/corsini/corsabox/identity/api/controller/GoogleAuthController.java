package com.corsini.corsabox.identity.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.corsini.corsabox.identity.api.*;
import com.corsini.corsabox.identity.api.response.TokenResponse;
import com.corsini.corsabox.identity.application.service.GoogleLoginUseCase;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {
    private final GoogleLoginUseCase google;
    private final RefreshCookie cookie;
    private final boolean enabled;
    private final String frontendUrl;

    public GoogleAuthController(GoogleLoginUseCase google, RefreshCookie cookie,
            @Value("${app.google.enabled:false}") boolean enabled,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.google = google;
        this.cookie = cookie;
        this.enabled = enabled;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @GetMapping("/config")
    public Config config() { return new Config(enabled); }

    @GetMapping("/pending")
    public Pending pending(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        var identity = GoogleLoginFlow.require(request.getSession(false)).identity();
        return new Pending(google.nextStep(identity), identity.email(), identity.firstName(), identity.lastName());
    }

    @PostMapping("/complete")
    public TokenResponse complete(@Valid @RequestBody Completion body,
            @RequestHeader(value = "Origin", required = false) String origin,
            HttpServletRequest request, HttpServletResponse response) {
        // Cookie-authenticated completion requires an exact trusted browser origin.
        if (!frontendUrl.equals(origin)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var session = request.getSession(false);
        GoogleLoginFlow.require(session);
        synchronized (session) {
            var identity = GoogleLoginFlow.require(session).identity();
            var tokens = google.complete(identity, body.username(), body.firstName(), body.lastName(), body.password());
            session.removeAttribute(GoogleLoginFlow.ATTRIBUTE);
            session.invalidate();
            cookie.write(response, tokens);
            response.setHeader("Cache-Control", "no-store");
            return new TokenResponse(tokens.accessToken());
        }
    }

    public record Config(boolean enabled) {}
    public record Pending(String step, String email, String firstName, String lastName) {}
    public record Completion(@Size(max = 20) String username, @Size(max = 255) String firstName,
                             @Size(max = 255) String lastName, @Size(max = 72) String password) {}
}
