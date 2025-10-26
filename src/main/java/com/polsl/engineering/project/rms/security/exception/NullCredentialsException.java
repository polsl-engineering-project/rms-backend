package com.polsl.engineering.project.rms.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class NullCredentialsException extends RuntimeException {
    public NullCredentialsException() {
        super("Credentials cannot be null");
    }
}
