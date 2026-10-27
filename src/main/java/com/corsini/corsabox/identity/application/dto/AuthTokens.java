package com.corsini.corsabox.identity.application.dto;

import java.time.Duration;

public record AuthTokens(String accessToken, String refreshToken, Duration refreshTtl) {}