package com.corsini.corsabox.identity.application.dto;

public record RegisterUserCommand(
    String firstName,
    String lastName,
    String username,
    String email
) {}

