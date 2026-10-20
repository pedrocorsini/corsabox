package com.corsini.corsabox.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Size(min = 3, max = 20) String username,
    @NotBlank @Email String email
) {}
