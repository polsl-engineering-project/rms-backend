package com.polsl.engineering.project.rms.security;

public record AuthenticationCredentials(String username, String encodedPassword) {
}
