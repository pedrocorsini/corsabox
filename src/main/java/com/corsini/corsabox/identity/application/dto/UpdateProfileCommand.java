package com.corsini.corsabox.identity.application.dto;

import java.util.UUID;

public record UpdateProfileCommand(
    UUID userId,
    String firstName,
    String lastName,
    String username
) {}
