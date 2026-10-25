package com.corsini.corsabox.identity.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmRegistrationRequest(
    @NotBlank @Email String email,
    @Pattern(regexp = "\\d{6}") String code
) {}