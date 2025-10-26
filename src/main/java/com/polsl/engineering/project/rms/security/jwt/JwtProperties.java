package com.polsl.engineering.project.rms.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String key, Long expirationMillis) {



}
