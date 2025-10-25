package com.polsl.engineering.project.rms.user.dto;

import com.polsl.engineering.project.rms.user.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String phoneNumber,
        Role role
) {
}
