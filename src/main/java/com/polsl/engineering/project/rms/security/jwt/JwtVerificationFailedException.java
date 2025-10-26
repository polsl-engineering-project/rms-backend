package com.polsl.engineering.project.rms.security.jwt;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class JwtVerificationFailedException extends RuntimeException {
    public JwtVerificationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

