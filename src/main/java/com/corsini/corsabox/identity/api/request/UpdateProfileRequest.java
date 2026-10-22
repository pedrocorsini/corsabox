package com.corsini.corsabox.identity.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Pattern(regexp = ".*\\S.*", message = "must not be blank") String firstName,
    @Pattern(regexp = ".*\\S.*", message = "must not be blank") String lastName,
    @Size(min = 3, max = 20) String username
) {}