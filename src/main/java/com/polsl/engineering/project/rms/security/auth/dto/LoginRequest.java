package com.polsl.engineering.project.rms.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull
        @NotBlank
        String username,

        @NotNull
        @NotBlank
        String password
) {}
