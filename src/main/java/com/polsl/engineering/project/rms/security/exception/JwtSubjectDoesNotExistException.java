package com.polsl.engineering.project.rms.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class JwtSubjectDoesNotExistException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "JWT subject with id '%s' does not exist.";

    public JwtSubjectDoesNotExistException(String id) {
        super(String.format(MESSAGE_TEMPLATE, id));
    }
}
