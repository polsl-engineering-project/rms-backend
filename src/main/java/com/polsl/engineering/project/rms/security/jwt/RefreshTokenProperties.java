package com.polsl.engineering.project.rms.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "refresh-tokens")
public record RefreshTokenProperties(
        int length,
        int maxPerUser,
        long validityDays
) {

    public Duration validityDuration() {
        return Duration.ofDays(validityDays);
    }
}
