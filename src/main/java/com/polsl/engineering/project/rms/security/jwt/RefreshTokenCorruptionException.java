package com.polsl.engineering.project.rms.security.jwt;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class RefreshTokenCorruptionException extends RuntimeException {
    public RefreshTokenCorruptionException(String message) {
        super(message);
    }
}
